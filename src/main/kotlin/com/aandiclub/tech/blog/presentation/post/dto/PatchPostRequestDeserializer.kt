package com.aandiclub.tech.blog.presentation.post.dto

import com.aandiclub.tech.blog.domain.post.PostStatus
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.exc.InvalidFormatException

class PatchPostRequestDeserializer : JsonDeserializer<PatchPostRequest>() {
	override fun deserialize(parser: JsonParser, ctxt: DeserializationContext): PatchPostRequest {
		val node = parser.codec.readTree<JsonNode>(parser)
		if (!node.isObject) {
			throw InvalidFormatException(parser, "post must be object", node, PatchPostRequest::class.java)
		}

		val authorNode = node.path("author").takeIf { !it.isMissingNode && !it.isNull }
			?: node.path("authorId").takeIf { !it.isMissingNode && !it.isNull }
		val author = authorNode?.let { parser.codec.treeToValue(it, PostAuthorRequest::class.java) }

		return PatchPostRequest(
			title = readText(node, "title"),
			contentMarkdown = readText(node, "contentMarkdown"),
			thumbnailUrl = readText(node, "thumbnailUrl"),
			author = author,
			status = readStatus(parser, node.path("status")),
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
