package com.aandiclub.tech.blog.common.api

import java.time.Instant

data class ApiResponse<T>(
	val success: Boolean,
	val data: T? = null,
	val error: ApiError? = null,
	val timestamp: Instant = Instant.now(),
) {
	companion object {
		fun <T> success(data: T): ApiResponse<T> = ApiResponse(success = true, data = data)

		fun failure(code: String, message: String): ApiResponse<Nothing> =
			ApiResponse(success = false, error = ApiError(code = code, message = message))
	}
}

data class ApiError(
	val code: String,
	val message: String,
)
