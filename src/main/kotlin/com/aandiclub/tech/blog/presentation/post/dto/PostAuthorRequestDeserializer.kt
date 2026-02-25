package com.aandiclub.tech.blog.presentation.post.dto

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.exc.InvalidFormatException

class PostAuthorRequestDeserializer : JsonDeserializer<PostAuthorRequest>() {
	override fun deserialize(parser: JsonParser, ctxt: DeserializationContext): PostAuthorRequest {
		val node = parser.codec.readTree<JsonNode>(parser)

		if (node.isTextual) {
			val id = node.asText().trim()
			if (id.isBlank()) {
				throw InvalidFormatException(parser, "author id must not be blank", node, String::class.java)
			}
			return PostAuthorRequest(id = id)
		}

		if (!node.isObject) {
			throw InvalidFormatException(parser, "author must be object or string", node, PostAuthorRequest::class.java)
		}

		val id = readText(node, "id")
			?: readText(node, "userId")
			?: throw InvalidFormatException(parser, "author.id is required", node, String::class.java)

		return PostAuthorRequest(
			id = id,
			nickname = readText(node, "nickname"),
			profileImageUrl = readText(node, "profileImageUrl")
				?: readText(node, "thumbnailUrl"),
		)
	}

	private fun readText(node: JsonNode, field: String): String? {
		val value = node.path(field)
		if (value.isMissingNode || value.isNull) return null
		val text = value.asText().trim()
		return text.ifBlank { null }
	}
}
