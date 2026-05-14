package com.aandiclub.tech.blog.common.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
class JacksonConfiguration {
	@Bean
	fun jackson2ObjectMapper(): ObjectMapper =
		ObjectMapper().findAndRegisterModules()
}
