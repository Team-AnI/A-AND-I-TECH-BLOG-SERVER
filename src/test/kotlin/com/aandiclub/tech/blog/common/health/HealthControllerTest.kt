package com.aandiclub.tech.blog.common.health

import io.kotest.core.spec.style.StringSpec
import org.springframework.test.web.reactive.server.WebTestClient

class HealthControllerTest : StringSpec({
	val client = WebTestClient.bindToController(HealthController()).build()

	"GET /health should return ok" {
		client.get()
			.uri("/health")
			.exchange()
			.expectStatus().isOk
			.expectBody()
			.jsonPath("$.status").isEqualTo("ok")
	}
})
