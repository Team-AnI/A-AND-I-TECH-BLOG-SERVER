package com.aandiclub.tech.blog.common.api.v2

import com.aandiclub.tech.blog.common.auth.AuthTokenService
import com.aandiclub.tech.blog.common.logging.ApiLogContext
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.time.format.DateTimeParseException

data class AiV2RequestContext(
	val deviceOs: String,
	val authenticate: String,
	val timestamp: Instant,
	val salt: String?,
	val requesterId: String,
)

@Component
class AiV2RequestContextResolver(
	private val authTokenService: AuthTokenService,
) {
	suspend fun resolve(exchange: ServerWebExchange): AiV2RequestContext {
		val headers = exchange.request.headers
		val deviceOs = headers.requiredHeader(DEVICE_OS_HEADER, AiV2ErrorCatalog.missingDeviceOs)
		val authenticate = headers.requiredHeader(AUTHENTICATE_HEADER, AiV2ErrorCatalog.missingAuthenticate)
		val timestampRaw = headers.requiredHeader(TIMESTAMP_HEADER, AiV2ErrorCatalog.missingTimestamp)
		val timestamp = try {
			Instant.parse(timestampRaw)
		} catch (_: DateTimeParseException) {
			throw AiV2ProtocolException(AiV2ErrorCatalog.invalidTimestamp, value = timestampRaw)
		}
		val requesterId = try {
			authTokenService.extractUserId(authenticate)
		} catch (exception: ResponseStatusException) {
			throw AiV2ProtocolException(
				descriptor = if (exception.statusCode.value() == HttpStatus.UNAUTHORIZED.value()) {
					AiV2ErrorCatalog.invalidAuthenticate
				} else {
					AiV2ErrorCatalog.internalServerError
				},
				value = exception.reason ?: authenticate,
				cause = exception,
			)
		}

		return AiV2RequestContext(
			deviceOs = deviceOs,
			authenticate = authenticate,
			timestamp = timestamp,
			salt = headers.getFirst(SALT_HEADER)?.trim()?.takeIf { it.isNotBlank() },
			requesterId = requesterId,
		).also { context ->
			exchange.attributes[CONTEXT_ATTRIBUTE] = context
			ApiLogContext.get(exchange)?.markAuthenticated(context.requesterId)
		}
	}

	private fun HttpHeaders.requiredHeader(name: String, descriptor: AiV2ErrorDescriptor): String =
		getFirst(name)?.trim()?.takeIf { it.isNotBlank() }
			?: throw AiV2ProtocolException(descriptor)

	companion object {
		const val CONTEXT_ATTRIBUTE = "aiV2RequestContext"
		const val DEVICE_OS_HEADER = "deviceOS"
		const val AUTHENTICATE_HEADER = "Authenticate"
		const val TIMESTAMP_HEADER = "timestamp"
		const val SALT_HEADER = "salt"
	}
}
