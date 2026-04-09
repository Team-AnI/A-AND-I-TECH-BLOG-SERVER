package com.aandiclub.tech.blog.common.auth

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("app.auth.jwt")
data class JwtAuthProperties(
	val issuerUri: String? = null,
	val jwkSetUri: String? = null,
	val sharedSecret: String? = null,
	val userIdClaim: String = "userId",
)
