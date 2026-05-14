package com.aandiclub.tech.blog.presentation.share

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.share")
data class SharePageProperties(
	val publicBaseUrl: String = "http://localhost:8080",
	val defaultOgImageUrl: String = "http://localhost:8080/og-default.png",
	val defaultDescription: String = "A&I 기술 블로그",
	val cache: ShareCacheProperties = ShareCacheProperties(),
)

data class ShareCacheProperties(
	val maxAgeSeconds: Long = 60,
	val sharedMaxAgeSeconds: Long = 300,
	val staleWhileRevalidateSeconds: Long = 600,
)
