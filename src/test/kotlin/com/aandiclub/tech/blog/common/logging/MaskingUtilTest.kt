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
				"passwordConfirm" to "secret",
				"token" to "token",
				"salt" to "salt",
				"secret" to "secret",
				"apiKey" to "api-key",
				"cookie" to "cookie",
				"session" to "session",
				"profile" to mapOf(
					"accessToken" to "access-token",
					"refreshToken" to "refresh-token",
				),
			),
		) as Map<*, *>

		masked["loginId"] shouldBe "han*****"
		masked["password"] shouldBe "****"
		masked["passwordConfirm"] shouldBe "****"
		masked["token"] shouldBe "****"
		masked["salt"] shouldBe "****"
		masked["secret"] shouldBe "****"
		masked["apiKey"] shouldBe "****"
		masked["cookie"] shouldBe "****"
		masked["session"] shouldBe "****"
		(masked["profile"] as Map<*, *>)["accessToken"] shouldBe "****"
		(masked["profile"] as Map<*, *>)["refreshToken"] shouldBe "****"
	}
})
