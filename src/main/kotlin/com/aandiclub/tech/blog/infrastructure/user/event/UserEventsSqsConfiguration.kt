package com.aandiclub.tech.blog.infrastructure.user.event

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsAsyncClient

@Configuration
@ConditionalOnProperty(prefix = "app.user-events", name = ["enabled"], havingValue = "true")
class UserEventsSqsConfiguration {
	@Bean
	fun sqsAsyncClient(userEventsProperties: UserEventsProperties): SqsAsyncClient =
		SqsAsyncClient.builder()
			.region(Region.of(userEventsProperties.region))
			.credentialsProvider(DefaultCredentialsProvider.builder().build())
			.build()
}
