package com.aandiclub.tech.blog.common.filter

import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import java.util.UUID

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class CorrelationIdFilter : WebFilter {
	override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
		val traceId = exchange.request.headers.getFirst(TRACE_ID_HEADER)
			?: exchange.request.headers.getFirst(ALT_TRACE_ID_HEADER)
			?: exchange.request.headers.getFirst(HEADER_NAME)
			?: UUID.randomUUID().toString()
		exchange.attributes[ATTRIBUTE_NAME] = traceId
		exchange.response.headers.set(HEADER_NAME, traceId)
		exchange.response.headers.set(TRACE_ID_HEADER, traceId)
		return chain.filter(exchange)
	}

	companion object {
		const val HEADER_NAME = "X-Correlation-Id"
		const val TRACE_ID_HEADER = "traceId"
		const val ALT_TRACE_ID_HEADER = "X-Trace-Id"
		const val ATTRIBUTE_NAME = "traceId"
	}
}
