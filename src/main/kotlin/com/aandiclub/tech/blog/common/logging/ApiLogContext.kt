package com.aandiclub.tech.blog.common.logging

import org.springframework.web.server.ServerWebExchange
import java.time.Instant

data class ApiLogContext(
	val traceId: String,
	val requestId: String,
	val startedAt: Instant = Instant.now(),
	val startedAtNanos: Long = System.nanoTime(),
	var requestBody: Any? = emptyMap<String, Any?>(),
	var responseBody: ByteArray? = null,
	var failureMessage: String? = null,
	var failureStatusCode: Int? = null,
	var errorCode: Any? = null,
	var authenticatedUserId: Any? = null,
	var actorRole: String? = null,
	var authenticated: Boolean = false,
) {
	fun markAuthenticated(userId: String, role: String = "USER") {
		authenticatedUserId = userId.toLongOrNull() ?: userId
		actorRole = role
		authenticated = true
	}

	fun markFailure(message: String, statusCode: Int, errorCode: Any? = null) {
		failureMessage = message
		failureStatusCode = statusCode
		this.errorCode = errorCode
	}

	companion object {
		const val ATTRIBUTE_NAME = "apiLogContext"
		const val REQUEST_ID_HEADER = "X-Request-Id"

		fun get(exchange: ServerWebExchange): ApiLogContext? = exchange.getAttribute(ATTRIBUTE_NAME)
	}
}
