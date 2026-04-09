package com.aandiclub.tech.blog.presentation.post.dto

import com.aandiclub.tech.blog.domain.post.PostStatus
import com.aandiclub.tech.blog.domain.post.PostType
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
				summary = readText(node, "summary"),
				contentMarkdown = readText(node, "contentMarkdown"),
				thumbnailUrl = readText(node, "thumbnailUrl"),
				author = author,
				collaborators = readCollaborators(parser, node),
				type = readType(parser, node.path("type")),
				status = readStatus(parser, node.path("status")),
			)
		}

	private fun readType(parser: JsonParser, node: JsonNode): PostType? {
		if (node.isMissingNode || node.isNull) return null
		return try {
			PostType.valueOf(node.asText())
		} catch (_: IllegalArgumentException) {
			throw InvalidFormatException(parser, "invalid post type", node, PostType::class.java)
		}
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

	private fun readCollaborators(parser: JsonParser, node: JsonNode): List<PostAuthorRequest>? {
		val collaboratorsNode = node.path("collaborators").takeIf { !it.isMissingNode && !it.isNull }
			?: node.path("collaboratorIds").takeIf { !it.isMissingNode && !it.isNull }
			?: return null
		if (!collaboratorsNode.isArray) {
			throw InvalidFormatException(parser, "collaborators must be array", collaboratorsNode, List::class.java)
		}
		return parser.codec.treeToValue(collaboratorsNode, Array<PostAuthorRequest>::class.java).toList()
	}
}
