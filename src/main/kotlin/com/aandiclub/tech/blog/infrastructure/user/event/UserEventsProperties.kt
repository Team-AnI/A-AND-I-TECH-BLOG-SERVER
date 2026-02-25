package com.aandiclub.tech.blog.infrastructure.user.event

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@ConfigurationProperties(prefix = "app.user-events")
@Validated
data class UserEventsProperties(
	val enabled: Boolean = false,
	val queueUrl: String = "",
	val region: String = "us-east-1",
	val waitTimeSeconds: Int = 20,
	val maxMessages: Int = 10,
	val pollDelayMs: Long = 1000,
)
