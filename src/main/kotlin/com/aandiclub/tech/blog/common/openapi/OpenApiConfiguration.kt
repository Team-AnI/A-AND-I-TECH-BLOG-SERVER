package com.aandiclub.tech.blog.common.openapi

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.servers.Server
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfiguration {
	@Bean
	fun blogOpenApi(): OpenAPI =
		OpenAPI()
				.info(
					Info()
						.title("Blog Service API")
						.description("Reactive blog API for posts and image uploads")
						.version("v1")
						.license(License().name("Proprietary"))
				)
				.servers(
					listOf(
						Server().url("https://api.aandiclub.com")
					)
				)
}
