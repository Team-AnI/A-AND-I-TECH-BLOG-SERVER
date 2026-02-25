package com.aandiclub.tech.blog.infrastructure.s3

import jakarta.validation.constraints.NotBlank
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@ConfigurationProperties(prefix = "app.s3")
@Validated
data class S3Properties(
	@field:NotBlank
	val bucket: String = "",
	@field:NotBlank
	val region: String = "us-east-1",
	val publicBaseUrl: String = "",
)
