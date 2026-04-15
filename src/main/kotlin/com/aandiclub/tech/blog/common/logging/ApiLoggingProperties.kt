package com.aandiclub.tech.blog.common.logging

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.api-log")
data class ApiLoggingProperties(
	val env: String = "local",
	val serviceName: String = "blog",
	val domainCode: Int = 6,
	val serviceVersion: String = "0.0.1-SNAPSHOT",
	val instanceId: String = "unknown-instance",
)
