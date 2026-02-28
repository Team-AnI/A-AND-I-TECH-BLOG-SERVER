package com.aandiclub.tech.blog.common.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain

@Configuration
@EnableWebFluxSecurity
class SecurityConfiguration {
	@Bean
	fun securityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain =
		http
			.csrf { it.disable() }
			.httpBasic { it.disable() }
			.formLogin { it.disable() }
			.authorizeExchange { exchanges ->
				exchanges.anyExchange().permitAll()
			}
			.build()
}
