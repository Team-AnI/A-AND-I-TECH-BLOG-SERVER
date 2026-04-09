package com.aandiclub.tech.blog.common.api.v2

import java.time.Instant

data class AiV2ApiResponse<T>(
	val success: Boolean,
	val data: T? = null,
	val error: AiV2ApiError? = null,
	val timestamp: Instant = Instant.now(),
) {
	companion object {
		fun <T> success(data: T): AiV2ApiResponse<T> = AiV2ApiResponse(success = true, data = data)

		fun failure(error: AiV2ApiError): AiV2ApiResponse<Nothing> =
			AiV2ApiResponse(success = false, error = error)
	}
}

data class AiV2ApiError(
	val code: Int,
	val message: String,
	val value: String = "",
	val alert: String = "",
)
