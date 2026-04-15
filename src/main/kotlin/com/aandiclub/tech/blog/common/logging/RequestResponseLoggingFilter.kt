package com.aandiclub.tech.blog.common.logging

import tools.jackson.core.type.TypeReference
import tools.jackson.databind.ObjectMapper
import net.logstash.logback.argument.StructuredArguments
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.http.MediaType
import org.springframework.http.codec.multipart.Part
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.http.server.reactive.ServerHttpRequestDecorator
import org.springframework.http.server.reactive.ServerHttpResponseDecorator
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import org.springframework.util.MultiValueMap
import java.util.UUID

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
class RequestResponseLoggingFilter(
	private val apiLogFactory: ApiLogFactory,
	private val objectMapper: ObjectMapper,
	private val maskingUtil: MaskingUtil,
) : WebFilter {
	private val logger = LoggerFactory.getLogger(RequestResponseLoggingFilter::class.java)

	override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
		if (!isApiRequest(exchange.request.path.value())) {
			return chain.filter(exchange)
		}

		val traceId = exchange.getAttribute<String>("traceId") ?: UUID.randomUUID().toString()
		val requestId = exchange.request.headers.getFirst(ApiLogContext.REQUEST_ID_HEADER)
			?.trim()
			?.takeIf { it.isNotBlank() }
			?: UUID.randomUUID().toString()
		val context = ApiLogContext(traceId = traceId, requestId = requestId)
		exchange.attributes[ApiLogContext.ATTRIBUTE_NAME] = context
		exchange.response.headers.set(ApiLogContext.REQUEST_ID_HEADER, requestId)

		return cacheRequestBody(exchange, context)
			.flatMap { cachedExchange ->
				val decoratedResponse = LoggingResponseDecorator(cachedExchange.response, context)
				val mutatedExchange = cachedExchange.mutate().response(decoratedResponse).build()
				chain.filter(mutatedExchange)
					.doOnError { throwable ->
						context.markFailure(
							message = throwable.message ?: "HTTP request failed",
							statusCode = 500,
						)
					}
					.doFinally {
						val entry = apiLogFactory.create(mutatedExchange, context)
						logger.atLevel(entry.level.toSlf4jLevel())
							.addArgument(StructuredArguments.entries(apiLogFactory.toStructuredFields(entry)))
							.log(entry.message)
					}
			}
	}

	private fun cacheRequestBody(exchange: ServerWebExchange, context: ApiLogContext): Mono<ServerWebExchange> {
		val contentType = exchange.request.headers.contentType
		if (isMultipart(contentType)) {
			return cacheMultipartMetadata(exchange, context)
		}

		if (!shouldCacheJsonBody(contentType)) {
			context.requestBody = emptyMap<String, Any?>()
			return Mono.just(exchange)
		}

		return DataBufferUtils.join(exchange.request.body)
			.defaultIfEmpty(exchange.response.bufferFactory().wrap(ByteArray(0)))
			.map { buffer ->
				val bytes = ByteArray(buffer.readableByteCount())
				buffer.read(bytes)
				DataBufferUtils.release(buffer)
				context.requestBody = decodeRequestBody(bytes)
				exchange.mutate()
					.request(CachedBodyRequest(exchange.request, bytes))
					.build()
			}
	}

	private fun cacheMultipartMetadata(exchange: ServerWebExchange, context: ApiLogContext): Mono<ServerWebExchange> =
		exchange.multipartData
			.map { parts: MultiValueMap<String, Part> ->
				context.requestBody = MultipartRequestLogMapper.toRequestBody(parts)
				exchange
			}

	private fun decodeRequestBody(bytes: ByteArray): Any? {
		if (bytes.isEmpty()) return emptyMap<String, Any?>()
		return runCatching {
			val parsed = objectMapper.readValue(bytes, object : TypeReference<LinkedHashMap<String, Any?>>() {})
			maskingUtil.maskBody(parsed)
		}.getOrElse {
			mapOf("raw" to maskingUtil.maskBody(String(bytes, Charsets.UTF_8)))
		}
	}

	private fun shouldCacheJsonBody(contentType: MediaType?): Boolean =
		contentType != null && (
			MediaType.APPLICATION_JSON.isCompatibleWith(contentType) ||
				contentType.subtype.endsWith("+json")
			)

	private fun isMultipart(contentType: MediaType?): Boolean =
		contentType != null && MediaType.MULTIPART_FORM_DATA.isCompatibleWith(contentType)

	private fun isApiRequest(path: String): Boolean = path.startsWith("/v1/") || path.startsWith("/v2/")

	private fun String.toSlf4jLevel(): Level = when (this) {
		"ERROR" -> Level.ERROR
		"WARN" -> Level.WARN
		"INFO" -> Level.INFO
		else -> Level.INFO
	}

	private class CachedBodyRequest(
		delegate: ServerHttpRequest,
		private val body: ByteArray,
	) : ServerHttpRequestDecorator(delegate) {
		override fun getBody(): Flux<DataBuffer> =
			if (body.isEmpty()) {
				Flux.empty()
			} else {
				Flux.defer { Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(body.copyOf())) }
			}
	}

	private class LoggingResponseDecorator(
		delegate: org.springframework.http.server.reactive.ServerHttpResponse,
		private val context: ApiLogContext,
	) : ServerHttpResponseDecorator(delegate) {
		override fun writeWith(body: org.reactivestreams.Publisher<out DataBuffer>): Mono<Void> {
			val bodyFlux = Flux.from(body)
			return DataBufferUtils.join(bodyFlux)
				.flatMap { joined ->
					val bytes = ByteArray(joined.readableByteCount())
					joined.read(bytes)
					DataBufferUtils.release(joined)
					context.responseBody = bytes
					super.writeWith(Mono.just(bufferFactory().wrap(bytes)))
				}
				.switchIfEmpty(super.writeWith(Mono.empty()))
		}

		override fun writeAndFlushWith(body: org.reactivestreams.Publisher<out org.reactivestreams.Publisher<out DataBuffer>>): Mono<Void> =
			writeWith(Flux.from(body).flatMapSequential { it })
	}
}
