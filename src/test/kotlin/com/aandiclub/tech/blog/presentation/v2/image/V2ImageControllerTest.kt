package com.aandiclub.tech.blog.presentation.v2.image

import com.aandiclub.tech.blog.common.api.v2.AiV2ErrorMapper
import com.aandiclub.tech.blog.common.api.v2.AiV2ExceptionHandler
import com.aandiclub.tech.blog.common.api.v2.AiV2RequestContextResolver
import com.aandiclub.tech.blog.common.auth.AuthTokenService
import com.aandiclub.tech.blog.presentation.image.ImageUploadService
import com.aandiclub.tech.blog.presentation.image.dto.ImageUploadResponse
import io.kotest.core.spec.style.StringSpec
import io.mockk.coEvery
import io.mockk.mockk
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.MediaType
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.BodyInserters

class V2ImageControllerTest : StringSpec({
	val service = mockk<ImageUploadService>()
	val authTokenService = mockk<AuthTokenService>()
	val headerResolver = AiV2RequestContextResolver(authTokenService)
	val webTestClient = WebTestClient.bindToController(V2ImageController(service, headerResolver))
		.controllerAdvice(AiV2ExceptionHandler(AiV2ErrorMapper()))
		.build()
	val authenticate = "Bearer v2-image-token"

	"POST /v2/posts/images should return wrapped upload metadata" {
		coEvery { authTokenService.extractUserId(eq(authenticate)) } returns "u-v2-image"
		coEvery { service.upload(any()) } returns ImageUploadResponse(
			url = "https://bucket.s3.us-east-1.amazonaws.com/posts/abc.png",
			key = "posts/abc.png",
			contentType = "image/png",
			size = 4,
		)

		val bodyBuilder = MultipartBodyBuilder()
		bodyBuilder.part(
			"file",
			object : ByteArrayResource(byteArrayOf(1, 2, 3, 4)) {
				override fun getFilename(): String = "abc.png"
			},
		).contentType(MediaType.IMAGE_PNG)

		webTestClient.post()
			.uri("/v2/posts/images")
			.header("deviceOS", "AOS")
			.header("Authenticate", authenticate)
			.header("timestamp", "2026-04-09T12:00:00Z")
			.contentType(MediaType.MULTIPART_FORM_DATA)
			.body(BodyInserters.fromMultipartData(bodyBuilder.build()))
			.exchange()
			.expectStatus().isOk
			.expectBody()
			.jsonPath("$.success").isEqualTo(true)
			.jsonPath("$.data.url").isEqualTo("https://bucket.s3.us-east-1.amazonaws.com/posts/abc.png")
			.jsonPath("$.data.key").isEqualTo("posts/abc.png")
			.jsonPath("$.data.contentType").isEqualTo("image/png")
			.jsonPath("$.data.size").isEqualTo(4)
	}
})
