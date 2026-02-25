package com.aandiclub.tech.blog.common

import com.aandiclub.tech.blog.common.filter.CorrelationIdFilter
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

class CorrelationIdFilterTest : StringSpec({
	val filter = CorrelationIdFilter()

	"should propagate provided correlation id" {
		val exchange = MockServerWebExchange.from(
			MockServerHttpRequest.get("/v1/posts")
				.header(CorrelationIdFilter.HEADER_NAME, "trace-abc")
				.build(),
		)

		filter.filter(exchange, WebFilterChain { Mono.empty() }).block()

		exchange.response.headers.getFirst(CorrelationIdFilter.HEADER_NAME) shouldBe "trace-abc"
		exchange.getAttribute<String>(CorrelationIdFilter.ATTRIBUTE_NAME) shouldBe "trace-abc"
	}

	"should generate correlation id when request header is absent" {
		val exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/health").build())

		filter.filter(exchange, WebFilterChain { Mono.empty() }).block()

		val headerValue = exchange.response.headers.getFirst(CorrelationIdFilter.HEADER_NAME)
		headerValue.shouldNotBeNull()
		headerValue.isNotBlank() shouldBe true
		exchange.getAttribute<String>(CorrelationIdFilter.ATTRIBUTE_NAME) shouldBe headerValue
	}
})
