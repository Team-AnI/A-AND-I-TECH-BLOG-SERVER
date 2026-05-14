package com.aandiclub.tech.blog.common.logging

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import tools.jackson.databind.json.JsonMapper

class ApiLogFactoryContextTest {
	@Test
	fun `should create ApiLogFactory with tools jackson object mapper`() {
		val factory = ApiLogFactory(
			properties = ApiLoggingProperties(),
			objectMapper = JsonMapper.builder().build(),
			maskingUtil = MaskingUtil(),
		)

		assertThat(factory).isNotNull
	}

	@Test
	fun `should derive common service domain from common error code and normalize fallback route`() {
		val factory = newFactory()
		val exchange = MockServerWebExchange.from(
			MockServerHttpRequest.get("/v2/posts/123?token=plain-token")
				.header("Authenticate", "Bearer authenticate-token")
				.header(HttpHeaders.AUTHORIZATION, "Bearer authorization-token")
				.header("salt", "plain-salt")
				.build(),
		)
		val context = ApiLogContext(traceId = "trace-1", requestId = "req-1")
		context.markFailure(message = "HTTP request failed", statusCode = 500, errorCode = 98801)

		val entry = factory.create(exchange, context)

		assertThat(entry.logType).isEqualTo(ApiLogType.API_ERROR.name)
		assertThat(entry.service.domain).isEqualTo("common")
		assertThat(entry.service.domainCode).isEqualTo(9)
		assertThat(entry.trace.traceId).isEqualTo("trace-1")
		assertThat(entry.trace.requestId).isEqualTo("req-1")
		assertThat(entry.http.route).isEqualTo("/v2/posts/{id}")
		assertThat(entry.response.error?.code).isEqualTo(98801)
		assertThat(entry.headers.Authenticate).isEqualTo("Bearer ****")
		assertThat(entry.headers.Authorization).isEqualTo("Bearer ****")
		assertThat(entry.headers.salt).isEqualTo("****")
		assertThat(entry.request.query["token"]).isEqualTo("****")
	}

	@Test
	fun `should mark successful blog operations as event logs`() {
		val factory = newFactory()
		val exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/v2/posts").build())
		val context = ApiLogContext(traceId = "trace-2", requestId = "req-2")
		context.markEvent(BlogEventType.BLOG_POST_CREATED.name, "post-1")

		val entry = factory.create(exchange, context)

		assertThat(entry.logType).isEqualTo(ApiLogType.EVENT.name)
		assertThat(entry.service.domain).isEqualTo("blog")
		assertThat(entry.service.domainCode).isEqualTo(6)
		assertThat(entry.event?.eventType).isEqualTo(BlogEventType.BLOG_POST_CREATED.name)
		assertThat(entry.event?.resourceId).isEqualTo("post-1")
	}

	@Test
	fun `should mark slow successful api logs`() {
		val factory = newFactory(ApiLoggingProperties(slowThresholdMs = 1))
		val exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/v2/posts").build())
		val context = ApiLogContext(
			traceId = "trace-3",
			requestId = "req-3",
			startedAtNanos = System.nanoTime() - 5_000_000,
		)

		val entry = factory.create(exchange, context)

		assertThat(entry.logType).isEqualTo(ApiLogType.API_SLOW.name)
	}

	private fun newFactory(properties: ApiLoggingProperties = ApiLoggingProperties()) =
		ApiLogFactory(
			properties = properties,
			objectMapper = JsonMapper.builder().build(),
			maskingUtil = MaskingUtil(),
		)
}
