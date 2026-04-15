package com.aandiclub.tech.blog.common.logging

import org.springframework.stereotype.Component

@Component
class MaskingUtil {
	fun maskAuthenticateHeader(value: String?): String? {
		val raw = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
		return if (raw.startsWith("Bearer ", ignoreCase = true)) {
			"Bearer ****"
		} else {
			"****"
		}
	}

	fun maskBody(value: Any?): Any? = maskValue(null, value)

	@Suppress("UNCHECKED_CAST")
	private fun maskValue(key: String?, value: Any?): Any? = when (value) {
		null -> null
		is Map<*, *> -> value.entries.associate { (entryKey, entryValue) ->
			entryKey.toString() to maskValue(entryKey.toString(), entryValue)
		}
		is Iterable<*> -> value.map { maskValue(key, it) }
		is Array<*> -> value.map { maskValue(key, it) }
		is String -> maskString(key, value)
		else -> value
	}

	private fun maskString(key: String?, value: String): String = when (key?.lowercase()) {
		"password", "accesstoken", "refreshtoken" -> "****"
		"loginid" -> maskLoginId(value)
		"authenticate", "authorization" -> maskAuthenticateHeader(value) ?: "****"
		else -> value
	}

	private fun maskLoginId(value: String): String {
		val trimmed = value.trim()
		if (trimmed.isEmpty()) return trimmed
		if (trimmed.length <= 3) return "*".repeat(trimmed.length)
		return trimmed.take(3) + "*".repeat(trimmed.length - 3)
	}
}
