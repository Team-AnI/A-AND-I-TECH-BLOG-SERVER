package com.aandiclub.tech.blog.infrastructure.s3

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3AsyncClient

@Configuration
class S3Configuration {
	@Bean
	fun s3AsyncClient(s3Properties: S3Properties): S3AsyncClient =
		S3AsyncClient.builder()
			.region(Region.of(s3Properties.region))
			.credentialsProvider(DefaultCredentialsProvider.builder().build())
			.build()
}
