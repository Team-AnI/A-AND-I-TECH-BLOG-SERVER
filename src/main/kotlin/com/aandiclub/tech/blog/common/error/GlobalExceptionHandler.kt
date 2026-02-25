package com.aandiclub.tech.blog.common.error

import com.aandiclub.tech.blog.common.api.ApiResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.FieldError
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.bind.support.WebExchangeBindException
import org.springframework.web.server.ResponseStatusException

@RestControllerAdvice
class GlobalExceptionHandler {

	@ExceptionHandler(ResponseStatusException::class)
	fun handleResponseStatusException(
		exception: ResponseStatusException,
	): ResponseEntity<ApiResponse<Nothing>> {
		val status = exception.statusCode
		return ResponseEntity.status(status).body(
			ApiResponse.failure(
				code = resolveCode(status.value(), (status as? HttpStatus)?.name),
				message = exception.reason ?: status.toString(),
			),
		)
	}

	@ExceptionHandler(WebExchangeBindException::class)
	fun handleValidationException(
		exception: WebExchangeBindException,
	): ResponseEntity<ApiResponse<Nothing>> {
		val message = exception.fieldErrors
			.takeIf { it.isNotEmpty() }
			?.joinToString("; ") { it.asMessage() }
			?: "validation failed"

		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
			ApiResponse.failure(
				code = "VALIDATION_FAILED",
				message = message,
			),
		)
	}

	@ExceptionHandler(Exception::class)
	fun handleUnhandledException(
		exception: Exception,
	): ResponseEntity<ApiResponse<Nothing>> =
		ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
			ApiResponse.failure(
				code = "INTERNAL_SERVER_ERROR",
				message = exception.message ?: "internal server error",
			),
		)

	private fun resolveCode(statusCode: Int, statusName: String?): String = when {
		!statusName.isNullOrBlank() -> statusName
		else -> statusCode.toString()
	}

	private fun FieldError.asMessage(): String = "$field: ${defaultMessage ?: "invalid value"}"
}
