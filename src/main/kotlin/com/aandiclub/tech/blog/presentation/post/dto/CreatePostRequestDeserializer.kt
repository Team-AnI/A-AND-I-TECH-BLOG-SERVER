package com.aandiclub.tech.blog.presentation.post.dto

import com.aandiclub.tech.blog.domain.post.PostStatus
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.exc.InvalidFormatException

class CreatePostRequestDeserializer : JsonDeserializer<CreatePostRequest>() {
	override fun deserialize(parser: JsonParser, ctxt: DeserializationContext): CreatePostRequest {
		val node = parser.codec.readTree<JsonNode>(parser)
		if (!node.isObject) {
			throw InvalidFormatException(parser, "post must be object", node, CreatePostRequest::class.java)
		}

		val authorNode = node.path("author").takeIf { !it.isMissingNode && !it.isNull }
			?: node.path("authorId").takeIf { !it.isMissingNode && !it.isNull }
			?: throw InvalidFormatException(parser, "author is required", node, PostAuthorRequest::class.java)

		val title = node.path("title").asText("")
		val contentMarkdown = node.path("contentMarkdown").asText("")
		val thumbnailUrl = readText(node, "thumbnailUrl")
		val status = readStatus(parser, node.path("status")) ?: PostStatus.Published
		val author = parser.codec.treeToValue(authorNode, PostAuthorRequest::class.java)

		return CreatePostRequest(
			title = title,
			contentMarkdown = contentMarkdown,
			thumbnailUrl = thumbnailUrl,
			author = author,
			status = status,
		)
	}

	private fun readStatus(parser: JsonParser, node: JsonNode): PostStatus? {
		if (node.isMissingNode || node.isNull) return null
		return try {
			PostStatus.valueOf(node.asText())
		} catch (_: IllegalArgumentException) {
			throw InvalidFormatException(parser, "invalid post status", node, PostStatus::class.java)
		}
	}

	private fun readText(node: JsonNode, field: String): String? {
		val value = node.path(field)
		if (value.isMissingNode || value.isNull) return null
		return value.asText()
	}
}
