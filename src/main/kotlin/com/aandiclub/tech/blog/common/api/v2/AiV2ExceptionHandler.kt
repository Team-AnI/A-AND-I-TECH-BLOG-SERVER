package com.aandiclub.tech.blog.common.api.v2

import com.aandiclub.tech.blog.common.logging.ApiLogContext
import com.aandiclub.tech.blog.presentation.v2.image.V2ImageController
import com.aandiclub.tech.blog.presentation.v2.post.V2PostController
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.FieldError
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.bind.support.WebExchangeBindException
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.ServerWebInputException

@RestControllerAdvice(basePackageClasses = [V2PostController::class, V2ImageController::class])
@Order(Ordered.HIGHEST_PRECEDENCE)
class AiV2ExceptionHandler(
	private val errorMapper: AiV2ErrorMapper,
) {
	@ExceptionHandler(AiV2ProtocolException::class)
	fun handleProtocolException(exception: AiV2ProtocolException, exchange: ServerWebExchange): ResponseEntity<AiV2ApiResponse<Nothing>> =
		buildErrorResponse(
			exchange = exchange,
			descriptor = exception.descriptor,
			message = exception.message,
		)

	@ExceptionHandler(ResponseStatusException::class)
	fun handleResponseStatusException(exception: ResponseStatusException, exchange: ServerWebExchange): ResponseEntity<AiV2ApiResponse<Nothing>> {
		val descriptor = errorMapper.map(exception)
		return buildErrorResponse(
			exchange = exchange,
			descriptor = descriptor,
			message = exception.reason ?: descriptor.message,
		)
	}

	@ExceptionHandler(WebExchangeBindException::class)
	fun handleValidationException(exception: WebExchangeBindException, exchange: ServerWebExchange): ResponseEntity<AiV2ApiResponse<Nothing>> {
		val value = exception.fieldErrors
			.takeIf { it.isNotEmpty() }
			?.joinToString("; ") { it.asMessage() }
			?: AiV2ErrorCatalog.validationFailed.message
		return buildErrorResponse(
			exchange = exchange,
			descriptor = AiV2ErrorCatalog.validationFailed,
			message = value,
		)
	}

	@ExceptionHandler(ServerWebInputException::class)
	fun handleServerWebInputException(
		exception: ServerWebInputException,
		exchange: ServerWebExchange,
	): ResponseEntity<AiV2ApiResponse<Nothing>> =
		buildErrorResponse(
			exchange = exchange,
			descriptor = AiV2ErrorCatalog.malformedBody,
			message = exception.reason ?: AiV2ErrorCatalog.malformedBody.message,
		)

	@ExceptionHandler(IllegalArgumentException::class)
	fun handleIllegalArgumentException(
		exception: IllegalArgumentException,
		exchange: ServerWebExchange,
	): ResponseEntity<AiV2ApiResponse<Nothing>> =
		buildErrorResponse(
			exchange = exchange,
			descriptor = AiV2ErrorCatalog.validationFailed,
			message = exception.message ?: AiV2ErrorCatalog.validationFailed.message,
		)

	@ExceptionHandler(Exception::class)
	fun handleUnhandledException(exception: Exception, exchange: ServerWebExchange): ResponseEntity<AiV2ApiResponse<Nothing>> =
		buildErrorResponse(
			exchange = exchange,
			descriptor = AiV2ErrorCatalog.blogInternalServerError,
			message = AiV2ErrorCatalog.blogInternalServerError.message,
		)

	private fun buildErrorResponse(
		exchange: ServerWebExchange,
		descriptor: AiV2ErrorDescriptor,
		message: String?,
	): ResponseEntity<AiV2ApiResponse<Nothing>> {
		val responseMessage = message?.takeIf { it.isNotBlank() } ?: descriptor.message
		ApiLogContext.get(exchange)?.markFailure(
			message = "HTTP request failed: $responseMessage",
			statusCode = descriptor.httpStatus.value(),
			errorCode = descriptor.code,
		)
		return ResponseEntity.status(descriptor.httpStatus).body(
			AiV2ApiResponse.failure(
				AiV2ApiError(
					code = descriptor.code,
					message = responseMessage,
					value = descriptor.value,
					alert = descriptor.alert,
				),
			),
		)
	}

	private fun FieldError.asMessage(): String = "$field: ${defaultMessage ?: "invalid value"}"
}
