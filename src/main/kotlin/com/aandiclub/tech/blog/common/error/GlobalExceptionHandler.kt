package com.aandiclub.tech.blog.common.error

import com.aandiclub.tech.blog.common.api.ApiResponse
import com.aandiclub.tech.blog.common.logging.ApiLogContext
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.FieldError
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.bind.support.WebExchangeBindException
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.ServerWebInputException

@RestControllerAdvice
class GlobalExceptionHandler {

	@ExceptionHandler(ResponseStatusException::class)
	fun handleResponseStatusException(
		exception: ResponseStatusException,
		exchange: ServerWebExchange,
	): ResponseEntity<ApiResponse<Nothing>> {
		val status = exception.statusCode
		ApiLogContext.get(exchange)?.markFailure(
			message = exception.reason ?: status.toString(),
			statusCode = status.value(),
			errorCode = resolveCode(status.value(), (status as? HttpStatus)?.name),
		)
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
		exchange: ServerWebExchange,
	): ResponseEntity<ApiResponse<Nothing>> {
		val message = exception.fieldErrors
			.takeIf { it.isNotEmpty() }
			?.joinToString("; ") { it.asMessage() }
			?: "validation failed"
		ApiLogContext.get(exchange)?.markFailure(
			message = message,
			statusCode = HttpStatus.BAD_REQUEST.value(),
			errorCode = "VALIDATION_FAILED",
		)

		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
			ApiResponse.failure(
				code = "VALIDATION_FAILED",
				message = message,
			),
		)
	}

	@ExceptionHandler(ServerWebInputException::class)
	fun handleServerWebInputException(
		exception: ServerWebInputException,
		exchange: ServerWebExchange,
	): ResponseEntity<ApiResponse<Nothing>> =
		ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
			ApiResponse.failure(
				code = "BAD_REQUEST",
				message = (exception.cause?.message ?: exception.reason ?: "Failed to read HTTP message").also { message ->
					ApiLogContext.get(exchange)?.markFailure(
						message = message,
						statusCode = HttpStatus.BAD_REQUEST.value(),
						errorCode = "BAD_REQUEST",
					)
				},
			),
		)

	@ExceptionHandler(Exception::class)
	fun handleUnhandledException(
		exception: Exception,
		exchange: ServerWebExchange,
	): ResponseEntity<ApiResponse<Nothing>> =
		ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
			ApiResponse.failure(
				code = "INTERNAL_SERVER_ERROR",
				message = (exception.message ?: "internal server error").also { message ->
					ApiLogContext.get(exchange)?.markFailure(
						message = message,
						statusCode = HttpStatus.INTERNAL_SERVER_ERROR.value(),
						errorCode = "INTERNAL_SERVER_ERROR",
					)
				},
			),
		)

	private fun resolveCode(statusCode: Int, statusName: String?): String = when {
		!statusName.isNullOrBlank() -> statusName
		else -> statusCode.toString()
	}

	private fun FieldError.asMessage(): String = "$field: ${defaultMessage ?: "invalid value"}"
}
