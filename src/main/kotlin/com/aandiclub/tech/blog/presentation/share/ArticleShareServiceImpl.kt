package com.aandiclub.tech.blog.presentation.share

import com.aandiclub.tech.blog.domain.post.PostType
import com.aandiclub.tech.blog.presentation.post.dto.PostResponse
import com.aandiclub.tech.blog.presentation.post.service.PostService
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.util.HtmlUtils
import java.util.UUID

@Service
class ArticleShareServiceImpl(
	private val postService: PostService,
	private val sharePageProperties: SharePageProperties,
) : ArticleShareService {

	override suspend fun getArticleSharePage(postId: UUID): ArticleSharePageResponse {
		val post = try {
			postService.get(postId)
				.takeIf { it.type == PostType.Blog && it.share != null }
		} catch (_: ResponseStatusException) {
			null
		}

		if (post == null) {
			val clientUrl = buildPublicUrl("/articles/$postId")
			return ArticleSharePageResponse(
				status = HttpStatus.NOT_FOUND,
				html = renderHtml(
					title = "게시글을 찾을 수 없습니다",
					description = sharePageProperties.defaultDescription,
					imageUrl = normalizeImageUrl(null),
					shareUrl = buildPublicUrl("/share/articles/$postId"),
					clientUrl = clientUrl,
				),
				cacheControl = "no-store, max-age=0",
			)
		}

		val share = post.share!!
		return ArticleSharePageResponse(
			status = HttpStatus.OK,
			html = renderHtml(
				title = post.title,
				description = share.description,
				imageUrl = share.imageUrl,
				shareUrl = share.shareUrl,
				clientUrl = share.clientUrl,
			),
			cacheControl = buildPublishedCacheControl(),
			lastModified = post.updatedAt,
		)
	}

	private fun buildPublishedCacheControl(): String {
		val cache = sharePageProperties.cache
		return "public, max-age=${cache.maxAgeSeconds}, s-maxage=${cache.sharedMaxAgeSeconds}, stale-while-revalidate=${cache.staleWhileRevalidateSeconds}"
	}

	private fun normalizeImageUrl(thumbnailUrl: String?): String {
		val candidate = thumbnailUrl?.trim().takeUnless { it.isNullOrEmpty() } ?: sharePageProperties.defaultOgImageUrl
		return if (ABSOLUTE_URL_REGEX.matches(candidate)) candidate else buildPublicUrl(if (candidate.startsWith("/")) candidate else "/$candidate")
	}

	private fun buildPublicUrl(path: String): String =
		org.springframework.web.util.UriComponentsBuilder.fromUriString(sharePageProperties.publicBaseUrl.trimEnd('/'))
			.path(path)
			.build()
			.toUriString()

	private fun renderHtml(
		title: String,
		description: String,
		imageUrl: String,
		shareUrl: String,
		clientUrl: String,
	): String {
		val escapedTitle = escape("$title | A&I")
		val escapedOgTitle = escape(title)
		val escapedDescription = escape(description)
		val escapedImageUrl = escape(imageUrl)
		val escapedShareUrl = escape(shareUrl)
		val escapedClientUrl = escape(clientUrl)
		return """
			<!DOCTYPE html>
			<html lang="ko">
			  <head>
			    <meta charset="utf-8" />
			    <title>$escapedTitle</title>
			    <meta name="robots" content="noindex,nofollow" />
			    <link rel="canonical" href="$escapedClientUrl" />
			    <meta property="og:title" content="$escapedOgTitle" />
			    <meta property="og:description" content="$escapedDescription" />
			    <meta property="og:image" content="$escapedImageUrl" />
			    <meta property="og:type" content="article" />
			    <meta property="og:url" content="$escapedShareUrl" />
			    <meta property="og:site_name" content="A&amp;I" />
			    <meta name="twitter:card" content="summary_large_image" />
			    <meta name="twitter:title" content="$escapedOgTitle" />
			    <meta name="twitter:description" content="$escapedDescription" />
			    <meta name="twitter:image" content="$escapedImageUrl" />
			    <meta http-equiv="refresh" content="0; url=$escapedClientUrl" />
			  </head>
			  <body></body>
			</html>
		""".trimIndent()
	}

	private fun escape(value: String): String = HtmlUtils.htmlEscape(value)

	companion object {
		private val ABSOLUTE_URL_REGEX = Regex("^https?://.+", RegexOption.IGNORE_CASE)
	}
}
