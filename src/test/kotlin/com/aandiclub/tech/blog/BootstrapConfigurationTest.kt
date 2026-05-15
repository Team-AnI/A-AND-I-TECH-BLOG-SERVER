package com.aandiclub.tech.blog

import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.r2dbc.spi.ConnectionFactory
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.core.env.Environment
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.EnabledIf

@SpringBootTest
@ActiveProfiles("test")
@EnabledIf(
	expression = "#{T(org.testcontainers.DockerClientFactory).instance().isDockerAvailable()}",
	loadContext = false,
	reason = "Requires Docker daemon for Testcontainers-backed R2DBC",
)
class BootstrapConfigurationTest {

	@Autowired
	lateinit var applicationContext: ApplicationContext

	@Autowired
	lateinit var environment: Environment

	@Test
	fun `test profile should define r2dbc testcontainers url`() {
		val url = environment.getProperty("spring.r2dbc.url")
		url.shouldNotBeNull()
		url.startsWith("r2dbc:tc:postgresql").shouldBeTrue()
	}

	@Test
	fun `context should expose reactive database beans`() {
		applicationContext.getBean(ConnectionFactory::class.java).shouldNotBeNull()
		applicationContext.getBean(DatabaseClient::class.java).shouldNotBeNull()
	}
}
