package com.aandiclub.tech.blog.presentation.share

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.string.shouldContain
import io.mockk.coEvery
import io.mockk.mockk
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.WebTestClient
import java.time.Instant
import java.util.UUID

class ArticleShareControllerTest : StringSpec({
	val service = mockk<ArticleShareService>()
	val webTestClient = WebTestClient.bindToController(ArticleShareController(service)).build()

	"GET /share/articles/{id} should return html with cache headers" {
		val postId = UUID.randomUUID()
		val updatedAt = Instant.parse("2026-05-06T00:00:00Z")
		coEvery { service.getArticleSharePage(postId) } returns
			ArticleSharePageResponse(
				status = HttpStatus.OK,
				html = "<!DOCTYPE html><html><head><title>ok</title></head><body></body></html>",
				cacheControl = "public, max-age=60, s-maxage=300, stale-while-revalidate=600",
				lastModified = updatedAt,
			)

		webTestClient.get()
			.uri("/share/articles/$postId")
			.exchange()
			.expectStatus().isOk
			.expectHeader().contentType("text/html;charset=UTF-8")
			.expectHeader().valueEquals(HttpHeaders.CACHE_CONTROL, "public, max-age=60, s-maxage=300, stale-while-revalidate=600")
			.expectHeader().valueEquals("X-Robots-Tag", "noindex, nofollow")
			.expectBody(String::class.java)
			.value { body ->
				body.shouldContain("<title>ok</title>")
			}
	}
})
