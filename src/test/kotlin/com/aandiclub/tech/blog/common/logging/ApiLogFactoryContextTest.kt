package com.aandiclub.tech.blog.common.logging

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
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
}
