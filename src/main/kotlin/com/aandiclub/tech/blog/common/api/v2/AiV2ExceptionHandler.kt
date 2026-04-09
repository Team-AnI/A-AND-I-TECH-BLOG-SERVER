package com.aandiclub.tech.blog.common.api.v2

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
import org.springframework.web.server.ServerWebInputException

@RestControllerAdvice(basePackageClasses = [V2PostController::class, V2ImageController::class])
@Order(Ordered.HIGHEST_PRECEDENCE)
class AiV2ExceptionHandler(
	private val errorMapper: AiV2ErrorMapper,
) {
	@ExceptionHandler(AiV2ProtocolException::class)
	fun handleProtocolException(exception: AiV2ProtocolException): ResponseEntity<AiV2ApiResponse<Nothing>> =
		buildErrorResponse(
			descriptor = exception.descriptor,
			value = exception.value,
		)

	@ExceptionHandler(ResponseStatusException::class)
	fun handleResponseStatusException(exception: ResponseStatusException): ResponseEntity<AiV2ApiResponse<Nothing>> {
		val descriptor = errorMapper.map(exception)
		return buildErrorResponse(
			descriptor = descriptor,
			value = exception.reason ?: descriptor.message,
		)
	}

	@ExceptionHandler(WebExchangeBindException::class)
	fun handleValidationException(exception: WebExchangeBindException): ResponseEntity<AiV2ApiResponse<Nothing>> {
		val value = exception.fieldErrors
			.takeIf { it.isNotEmpty() }
			?.joinToString("; ") { it.asMessage() }
			?: AiV2ErrorCatalog.validationFailed.message
		return buildErrorResponse(
			descriptor = AiV2ErrorCatalog.validationFailed,
			value = value,
		)
	}

	@ExceptionHandler(ServerWebInputException::class)
	fun handleServerWebInputException(exception: ServerWebInputException): ResponseEntity<AiV2ApiResponse<Nothing>> =
		buildErrorResponse(
			descriptor = AiV2ErrorCatalog.malformedBody,
			value = exception.cause?.message ?: exception.reason ?: AiV2ErrorCatalog.malformedBody.message,
		)

	@ExceptionHandler(IllegalArgumentException::class)
	fun handleIllegalArgumentException(exception: IllegalArgumentException): ResponseEntity<AiV2ApiResponse<Nothing>> =
		buildErrorResponse(
			descriptor = AiV2ErrorCatalog.validationFailed,
			value = exception.message ?: AiV2ErrorCatalog.validationFailed.message,
		)

	@ExceptionHandler(Exception::class)
	fun handleUnhandledException(exception: Exception): ResponseEntity<AiV2ApiResponse<Nothing>> =
		buildErrorResponse(
			descriptor = AiV2ErrorCatalog.internalServerError,
			value = exception.message ?: AiV2ErrorCatalog.internalServerError.message,
		)

	private fun buildErrorResponse(
		descriptor: AiV2ErrorDescriptor,
		value: String,
	): ResponseEntity<AiV2ApiResponse<Nothing>> =
		ResponseEntity.status(descriptor.httpStatus).body(
			AiV2ApiResponse.failure(
				AiV2ApiError(
					code = descriptor.code,
					message = descriptor.message,
					value = value,
					alert = descriptor.alert,
				),
			),
		)

	private fun FieldError.asMessage(): String = "$field: ${defaultMessage ?: "invalid value"}"
}
