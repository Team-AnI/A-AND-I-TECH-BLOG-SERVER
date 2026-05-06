package com.aandiclub.tech.blog.presentation.share

import com.aandiclub.tech.blog.domain.post.Post
import com.aandiclub.tech.blog.domain.post.PostStatus
import com.aandiclub.tech.blog.domain.post.PostType
import com.aandiclub.tech.blog.presentation.post.dto.PostShareResponse
import org.springframework.stereotype.Component
import org.springframework.web.util.UriComponentsBuilder

@Component
class PostShareMetadataFactory(
	private val sharePageProperties: SharePageProperties = SharePageProperties(),
) {
	fun create(post: Post): PostShareResponse? {
		if (post.status != PostStatus.Published || post.type != PostType.Blog) {
			return null
		}

		return PostShareResponse(
			shareUrl = buildPublicUrl("/share/articles/${post.id}"),
			clientUrl = buildPublicUrl("/articles/${post.id}"),
			title = post.title,
			description = resolveDescription(post),
			imageUrl = normalizeImageUrl(post.thumbnailUrl),
		)
	}

	private fun resolveDescription(post: Post): String {
		val summary = post.summary.trim()
		if (summary.isNotEmpty()) {
			return truncate(summary, MAX_DESCRIPTION_LENGTH)
		}

		val excerpt = summarizeMarkdown(post.contentMarkdown)
		if (excerpt.isNotEmpty()) {
			return truncate(excerpt, MAX_DESCRIPTION_LENGTH)
		}

		return sharePageProperties.defaultDescription
	}

	private fun summarizeMarkdown(markdown: String): String =
		markdown
			.replace(IMAGE_REGEX, " ")
			.replace(LINK_REGEX, "$1")
			.replace(CODE_FENCE_REGEX, " ")
			.replace(INLINE_CODE_REGEX, "$1")
			.replace(MARKDOWN_DECORATION_REGEX, " ")
			.replace(WHITESPACE_REGEX, " ")
			.trim()

	private fun normalizeImageUrl(thumbnailUrl: String?): String {
		val candidate = thumbnailUrl?.trim().takeUnless { it.isNullOrEmpty() } ?: sharePageProperties.defaultOgImageUrl
		return toAbsoluteUrl(candidate)
	}

	private fun buildPublicUrl(path: String): String =
		UriComponentsBuilder.fromUriString(sharePageProperties.publicBaseUrl.trimEnd('/'))
			.path(path)
			.build()
			.toUriString()

	private fun toAbsoluteUrl(value: String): String {
		if (ABSOLUTE_URL_REGEX.matches(value)) {
			return value
		}

		val normalizedPath = if (value.startsWith("/")) value else "/$value"
		return buildPublicUrl(normalizedPath)
	}

	private fun truncate(value: String, maxLength: Int): String =
		if (value.length <= maxLength) value else value.take(maxLength).trimEnd() + "…"

	companion object {
		private const val MAX_DESCRIPTION_LENGTH = 200
		private val ABSOLUTE_URL_REGEX = Regex("^https?://.+", RegexOption.IGNORE_CASE)
		private val IMAGE_REGEX = Regex("!\\[[^\\]]*]\\([^)]*\\)")
		private val LINK_REGEX = Regex("\\[([^\\]]+)]\\([^)]*\\)")
		private val CODE_FENCE_REGEX = Regex("```[\\s\\S]*?```")
		private val INLINE_CODE_REGEX = Regex("`([^`]*)`")
		private val MARKDOWN_DECORATION_REGEX = Regex("[#>*_~\\-]+")
		private val WHITESPACE_REGEX = Regex("\\s+")
	}
}
