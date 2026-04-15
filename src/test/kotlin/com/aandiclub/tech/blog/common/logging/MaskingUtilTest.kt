package com.aandiclub.tech.blog.common.logging

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class MaskingUtilTest : StringSpec({
	val maskingUtil = MaskingUtil()

	"should mask Authenticate bearer header" {
		maskingUtil.maskAuthenticateHeader("Bearer abc.def.ghi") shouldBe "Bearer ****"
		maskingUtil.maskAuthenticateHeader(null) shouldBe null
	}

	"should recursively mask sensitive request body fields" {
		val masked = maskingUtil.maskBody(
			mapOf(
				"loginId" to "hansw123",
				"password" to "secret",
				"profile" to mapOf(
					"accessToken" to "access-token",
					"refreshToken" to "refresh-token",
				),
			),
		) as Map<*, *>

		masked["loginId"] shouldBe "han*****"
		masked["password"] shouldBe "****"
		(masked["profile"] as Map<*, *>)["accessToken"] shouldBe "****"
		(masked["profile"] as Map<*, *>)["refreshToken"] shouldBe "****"
	}
})
