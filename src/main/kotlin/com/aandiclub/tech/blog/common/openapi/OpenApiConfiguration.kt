package com.aandiclub.tech.blog.common.openapi

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.media.StringSchema
import io.swagger.v3.oas.models.parameters.Parameter
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import org.springdoc.core.customizers.OperationCustomizer
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.method.HandlerMethod

@Configuration
class OpenApiConfiguration {
	companion object {
		const val BEARER_AUTH_SCHEME = "bearerAuth"
		const val AI_V2_AUTH_SCHEME = "authenticateHeader"
		const val V1_GROUP = "v1"
		const val V2_GROUP = "v2"
	}

	@Bean
	fun blogOpenApi(): OpenAPI =
		OpenAPI()
			.info(
				Info()
					.title("Blog Service API")
					.description("Reactive blog API for posts and image uploads")
					.version("all")
					.license(License().name("Proprietary")),
			)
			.servers(
				listOf(
					Server().url("https://api.aandiclub.com"),
				),
			)
			.components(
				Components()
					.addSecuritySchemes(
						BEARER_AUTH_SCHEME,
						SecurityScheme()
							.type(SecurityScheme.Type.HTTP)
							.scheme("bearer")
							.bearerFormat("JWT"),
					)
					.addSecuritySchemes(
						AI_V2_AUTH_SCHEME,
						SecurityScheme()
							.type(SecurityScheme.Type.APIKEY)
							.`in`(SecurityScheme.In.HEADER)
							.name("Authenticate")
							.description("Bearer {accessToken}"),
					),
			)

	@Bean
	fun v1GroupedOpenApi(): GroupedOpenApi =
		GroupedOpenApi.builder()
			.group(V1_GROUP)
			.pathsToMatch("/v1/**")
			.addOpenApiCustomizer { openApi ->
				openApi.info(
					Info()
						.title("Blog Service API - v1")
						.description("Legacy v1 API for blog posts and image uploads")
						.version("v1")
						.license(License().name("Proprietary")),
				)
			}
			.build()

	@Bean
	fun v2GroupedOpenApi(v2OperationCustomizer: OperationCustomizer): GroupedOpenApi =
		GroupedOpenApi.builder()
			.group(V2_GROUP)
			.pathsToMatch("/v2/**")
			.addOpenApiCustomizer { openApi ->
				openApi.info(
					Info()
						.title("Blog Service API - v2")
						.description("A&I v2 protocol API for blog posts and image uploads")
						.version("v2")
						.license(License().name("Proprietary")),
				)
			}
			.addOperationCustomizer(v2OperationCustomizer)
			.build()

	@Bean
	fun v2OperationCustomizer(): OperationCustomizer = OperationCustomizer { operation, _: HandlerMethod ->
		val parameters = operation.parameters ?: mutableListOf()
		parameters.ensureHeaderParameter(
			name = "deviceOS",
			required = true,
			description = "Client device OS",
		)
		parameters.ensureHeaderParameter(
			name = "timestamp",
			required = true,
			description = "Request timestamp in ISO-8601 instant format",
			example = "2026-04-09T12:00:00Z",
		)
		parameters.ensureHeaderParameter(
			name = "salt",
			required = false,
			description = "Optional request salt",
		)
		operation.parameters = parameters
		operation.security = listOf(SecurityRequirement().addList(AI_V2_AUTH_SCHEME))
		operation
	}

	private fun MutableList<Parameter>.ensureHeaderParameter(
		name: String,
		required: Boolean,
		description: String,
		example: String? = null,
	) {
		if (any { it.name == name && it.`in` == "header" }) return
		add(
			Parameter()
				.`in`("header")
				.name(name)
				.required(required)
				.description(description)
				.schema(StringSchema())
				.apply {
					if (example != null) {
						this.example = example
					}
				},
		)
	}
}
