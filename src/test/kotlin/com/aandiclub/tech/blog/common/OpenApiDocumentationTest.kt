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
	"v3 api docs should include posts and images endpoints" {
		val result = WebTestClient.bindToServer()
			.baseUrl("http://localhost:$port")
			.build()
			.get()
			.uri("/v3/api-docs")
			.exchange()
			.expectStatus().isOk
			.expectBody()
			.returnResult()
			.responseBody
			?.toString(Charsets.UTF_8)
			.orEmpty()

		result.shouldContain("/v1/posts")
		result.shouldContain("/v1/posts/{postId}")
		result.shouldContain("/v1/posts/images")
	}
})
