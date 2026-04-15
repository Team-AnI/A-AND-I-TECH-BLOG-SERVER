package com.aandiclub.tech.blog.common.logging

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.reactive.HandlerMapping
import org.springframework.web.server.ServerWebExchange
import java.time.Instant

@Component
class ApiLogFactory(
	private val properties: ApiLoggingProperties,
	private val objectMapper: ObjectMapper,
	private val maskingUtil: MaskingUtil,
) {
	fun create(exchange: ServerWebExchange, context: ApiLogContext): ApiLogEntry {
		val statusCode = context.failureStatusCode
			?: exchange.response.statusCode?.value()
			?: 200
		val route = exchange.getAttribute<Any>(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE)
			?.toString()
			?: exchange.request.path.value()
		val response = buildResponse(context, statusCode)
		val message = buildMessage(response, context)
		val level = buildLevel(statusCode)
		val feature = resolveFeature(route)
		val outcome = if (response.success) "success" else "fail"

		return ApiLogEntry(
			timestamp = Instant.now(),
			level = level,
			logType = if (response.success) "API" else "API_ERROR",
			message = message,
			env = properties.env,
			service = ApiLogService(
				name = properties.serviceName,
				domainCode = properties.domainCode,
				version = properties.serviceVersion,
				instanceId = properties.instanceId,
			),
			trace = ApiLogTrace(
				traceId = context.traceId,
				requestId = context.requestId,
			),
			http = ApiLogHttp(
				method = exchange.request.method?.name() ?: "UNKNOWN",
				path = exchange.request.path.value(),
				route = route,
				statusCode = statusCode,
				latencyMs = ((System.nanoTime() - context.startedAtNanos) / 1_000_000).coerceAtLeast(0),
			),
			headers = ApiLogHeaders(
				deviceOS = exchange.request.headers.getFirst("deviceOS"),
				Authenticate = maskingUtil.maskAuthenticateHeader(exchange.request.headers.getFirst("Authenticate")),
				timestamp = exchange.request.headers.getFirst("timestamp"),
				salt = exchange.request.headers.getFirst("salt"),
			),
			client = ApiLogClient(
				ip = resolveClientIp(exchange.request.headers, exchange),
				userAgent = exchange.request.headers.getFirst(HttpHeaders.USER_AGENT),
				appVersion = exchange.request.headers.getFirst("appVersion")
					?: exchange.request.headers.getFirst("App-Version"),
			),
			actor = ApiLogActor(
				userId = context.authenticatedUserId,
				role = context.actorRole,
				isAuthenticated = context.authenticated,
			),
			request = ApiLogRequest(
				query = exchange.request.queryParams.toSingleValueMap().mapValues { (_, value) -> value },
				pathVariables = resolvePathVariables(exchange),
				body = maskingUtil.maskBody(context.requestBody) ?: emptyMap<String, Any?>(),
			),
			response = response,
			tags = listOf(
				properties.serviceName,
				feature,
				outcome,
				resolveDetail(route, exchange.request.method?.name()),
			),
		)
	}

	fun toStructuredFields(entry: ApiLogEntry): Map<String, Any?> =
		objectMapper.convertValue(entry, object : TypeReference<LinkedHashMap<String, Any?>>() {})
			.apply {
				remove("@timestamp")
				remove("level")
				remove("message")
			}

	private fun buildResponse(context: ApiLogContext, statusCode: Int): ApiLogResponse {
		val rawBody = context.responseBody
		val payload = rawBody
			?.takeIf { it.isNotEmpty() }
			?.let { decodeJsonObject(it) }

		val success = payload?.get("success") as? Boolean ?: (statusCode in 200..399)
		val responseTimestamp = payload?.get("timestamp")?.toString() ?: Instant.now().toString()
		val data = if (success) maskingUtil.maskBody(payload?.get("data") ?: payload) else null
		val rawError = payload?.get("error")
		val error = if (success) {
			null
		} else {
			val errorMap = rawError as? Map<*, *>
			val message = errorMap?.get("message")?.toString()
				?: context.failureMessage
				?: "request failed"
			ApiLogResponseError(
				code = errorMap?.get("code") ?: context.errorCode,
				message = message,
				value = errorMap?.get("value")?.toString() ?: message,
				alert = errorMap?.get("alert")?.toString() ?: message,
			)
		}

		return ApiLogResponse(
			success = success,
			data = data,
			error = error,
			timestamp = responseTimestamp,
		)
	}

	private fun buildMessage(response: ApiLogResponse, context: ApiLogContext): String = if (response.success) {
		"HTTP request completed"
	} else {
		context.failureMessage
			?: response.error?.message?.let { "HTTP request failed: $it" }
			?: "HTTP request failed"
	}

	private fun buildLevel(statusCode: Int): String = when {
		statusCode >= 500 -> "ERROR"
		statusCode >= 400 -> "WARN"
		else -> "INFO"
	}

	@Suppress("UNCHECKED_CAST")
	private fun decodeJsonObject(bytes: ByteArray): Map<String, Any?>? = runCatching {
		objectMapper.readValue(bytes, object : TypeReference<LinkedHashMap<String, Any?>>() {})
	}.getOrNull()

	@Suppress("UNCHECKED_CAST")
	private fun resolvePathVariables(exchange: ServerWebExchange): Map<String, Any?> =
		(exchange.getAttribute<Map<String, String>>(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE) ?: emptyMap())
			.mapValues { (_, value) -> value }

	private fun resolveFeature(route: String): String =
		route.trim('/').split('/').drop(1).firstOrNull { it.isNotBlank() && !it.startsWith("{") } ?: "api"

	private fun resolveDetail(route: String, method: String?): String {
		val tokens = route.trim('/').split('/').filter { it.isNotBlank() }
		val staticTokens = tokens.filterNot { it.startsWith("{") }
		return when {
			staticTokens.isEmpty() -> "request"
			staticTokens.last() == "images" -> "upload"
			staticTokens.last() == "collaborators" -> "collaborators"
			staticTokens.last() == "drafts" -> "drafts"
			staticTokens.last() == "me" && staticTokens.size >= 3 && staticTokens[staticTokens.lastIndex - 1] == "drafts" -> "my-drafts"
			staticTokens.last() == "me" -> "me"
			staticTokens.size == 2 && method == "POST" -> "create"
			staticTokens.size == 2 && method == "GET" -> "list"
			method == "PATCH" -> "patch"
			method == "DELETE" -> "delete"
			method == "GET" -> "detail"
			else -> staticTokens.last()
		}
	}

	private fun resolveClientIp(headers: HttpHeaders, exchange: ServerWebExchange): String? =
		headers.getFirst("X-Forwarded-For")
			?.substringBefore(',')
			?.trim()
			?.takeIf { it.isNotBlank() }
			?: headers.getFirst("X-Real-IP")?.trim()?.takeIf { it.isNotBlank() }
			?: exchange.request.remoteAddress?.address?.hostAddress
}
