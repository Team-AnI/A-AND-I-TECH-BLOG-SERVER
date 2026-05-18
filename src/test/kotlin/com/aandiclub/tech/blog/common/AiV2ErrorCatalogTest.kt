package com.aandiclub.tech.blog.common

import com.aandiclub.tech.blog.common.api.v2.AiV2ErrorCatalog
import com.aandiclub.tech.blog.common.api.v2.AiV2ErrorCategory
import com.aandiclub.tech.blog.common.api.v2.AiV2ErrorService
import com.aandiclub.tech.blog.common.api.v2.AiV2ErrorSeverity
import com.aandiclub.tech.blog.common.api.v2.AiV2ErrorStatus
import com.aandiclub.tech.blog.common.api.v2.AiV2ErrorMapper
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

class AiV2ErrorCatalogTest : StringSpec({
	"catalog should contain v2_0_1 blog operation monitoring codes" {
		val codes = AiV2ErrorCatalog.all.map { it.code }

		codes shouldContainAll listOf(64301, 64501, 64801, 64802, 64803, 64804, 64805, 68801)
	}

	"catalog should contain v2_0_1 common monitoring codes and deprecate legacy internal code" {
		val codes = AiV2ErrorCatalog.all.map { it.code }

		codes shouldContainAll listOf(90001, 93001, 95001, 90701, 98801, 90801)
		AiV2ErrorCatalog.badRequest.status shouldBe AiV2ErrorStatus.DEPRECATED
		AiV2ErrorCatalog.commonValidationError.code shouldBe 93001
		AiV2ErrorCatalog.deprecatedInternalServerError.status shouldBe AiV2ErrorStatus.DEPRECATED
		AiV2ErrorCatalog.internalServerError.code shouldBe 98801
	}

	"catalog codes should be unique and match service domain code" {
		val codes = AiV2ErrorCatalog.all.map { it.code }

		codes.distinct().size shouldBe codes.size

		AiV2ErrorCatalog.all.forEach { descriptor ->
			descriptor.code.toString().first().digitToInt() shouldBe descriptor.service.domainCode
		}
	}

	"operation failure severities should follow monitoring contract" {
		listOf(
			AiV2ErrorCatalog.postCreateFailed,
			AiV2ErrorCatalog.postUpdateFailed,
			AiV2ErrorCatalog.postDeleteFailed,
			AiV2ErrorCatalog.postPublishFailed,
			AiV2ErrorCatalog.postUnpublishFailed,
		).forEach { descriptor ->
			descriptor.service shouldBe AiV2ErrorService.BLOG
			descriptor.category shouldBe AiV2ErrorCategory.INTERNAL_SYSTEM
			descriptor.severity shouldBe AiV2ErrorSeverity.HIGH
		}

		AiV2ErrorCatalog.blogInternalServerError.severity shouldBe AiV2ErrorSeverity.CRITICAL
		AiV2ErrorCatalog.internalServerError.severity shouldBe AiV2ErrorSeverity.CRITICAL
		AiV2ErrorCatalog.externalSystemUnavailable.severity shouldBe AiV2ErrorSeverity.HIGH
	}

	"v2 mapper should route blog internal errors to blog monitoring code" {
		val descriptor = AiV2ErrorMapper().map(ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR))

		descriptor.code shouldBe 68801
		descriptor.service shouldBe AiV2ErrorService.BLOG
	}

	"v2 mapper should use common validation code for generic bad request fallback" {
		val descriptor = AiV2ErrorMapper().map(ResponseStatusException(HttpStatus.BAD_REQUEST, "bad request"))

		descriptor.code shouldBe 93001
		descriptor.value shouldBe "COMMON_VALIDATION_ERROR"
	}
})
