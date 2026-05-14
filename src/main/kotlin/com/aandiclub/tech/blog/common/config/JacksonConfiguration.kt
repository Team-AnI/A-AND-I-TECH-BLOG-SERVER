package com.aandiclub.tech.blog.common.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.json.JsonMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class JacksonConfiguration {
	@Bean
	fun objectMapper(): ObjectMapper = JsonMapper.builder()
		.findAndAddModules()
		.build()
}
