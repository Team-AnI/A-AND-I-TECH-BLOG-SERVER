package com.aandiclub.tech.blog.presentation.share

import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/share/articles")
class ArticleShareController(
	private val articleShareService: ArticleShareService,
) {
	@GetMapping("/{postId}", produces = ["text/html;charset=UTF-8"])
	suspend fun get(@PathVariable postId: UUID): ResponseEntity<String> {
		val sharePage = articleShareService.getArticleSharePage(postId)
		return ResponseEntity.status(sharePage.status)
			.contentType(TEXT_HTML_UTF8)
			.header(HttpHeaders.CACHE_CONTROL, sharePage.cacheControl)
			.header("X-Robots-Tag", "noindex, nofollow")
			.apply {
				sharePage.lastModified?.let { lastModified(it.toEpochMilli()) }
			}
			.body(sharePage.html)
	}

	companion object {
		private val TEXT_HTML_UTF8 = MediaType.parseMediaType("text/html;charset=UTF-8")
	}
}
