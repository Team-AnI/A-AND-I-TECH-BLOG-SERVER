package com.aandiclub.tech.blog.common

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.string.shouldContain
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest(
	webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
	properties = [
		"spring.r2dbc.url=r2dbc:postgresql://localhost:5432/tech_blog",
		"spring.r2dbc.username=tech_blog",
		"spring.r2dbc.password=tech_blog",
		"spring.flyway.enabled=false",
	],
)
class OpenApiDocumentationTest(
	@LocalServerPort private val port: Int,
) : StringSpec({
	fun fetch(path: String): String = WebTestClient.bindToServer()
		.baseUrl("http://localhost:$port")
		.build()
		.get()
		.uri(path)
		.exchange()
		.expectStatus().isOk
		.expectBody()
		.returnResult()
		.responseBody
		?.toString(Charsets.UTF_8)
		.orEmpty()

	"v1 api docs should include only v1 blog endpoints" {
		val result = fetch("/v3/api-docs/v1")

		result.shouldContain("/v1/posts")
		result.shouldContain("/v1/posts/{postId}")
		result.shouldContain("/v1/posts/drafts/me")
		result.shouldContain("/v1/posts/images")
		result.shouldContain("bearerAuth")
	}

	"v2 api docs should include only v2 blog endpoints and A&I headers" {
		val result = fetch("/v3/api-docs/v2")

		result.shouldContain("/v2/posts")
		result.shouldContain("/v2/posts/{postId}")
		result.shouldContain("/v2/posts/drafts/me")
		result.shouldContain("/v2/posts/images")
		result.shouldContain("authenticateHeader")
		result.shouldContain("deviceOS")
		result.shouldContain("timestamp")
		result.shouldContain("salt")
	}
})
