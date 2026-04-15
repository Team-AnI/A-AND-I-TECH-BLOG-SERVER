package com.aandiclub.tech.blog.common.logging

import org.springframework.http.codec.multipart.FilePart
import org.springframework.http.codec.multipart.FormFieldPart
import org.springframework.http.codec.multipart.Part
import org.springframework.util.MultiValueMap

object MultipartRequestLogMapper {
	fun toRequestBody(parts: MultiValueMap<String, Part>): Map<String, Any?> =
		mapOf(
			"type" to "multipart/form-data",
			"partCount" to parts.values.sumOf { it.size },
			"parts" to parts.values
				.flatten()
				.map { part ->
					mapOf(
						"name" to part.name(),
						"kind" to resolveKind(part),
						"filename" to (part as? FilePart)?.filename(),
						"contentType" to part.headers().contentType?.toString(),
						"contentLength" to part.headers().contentLength.takeIf { it >= 0 },
					)
				},
		)

	private fun resolveKind(part: Part): String = when (part) {
		is FilePart -> "file"
		is FormFieldPart -> "field"
		else -> "part"
	}
}
