package com.aandiclub.tech.blog.presentation.v2.image

import com.aandiclub.tech.blog.common.api.v2.AiV2ApiResponse
import com.aandiclub.tech.blog.common.api.v2.AiV2RequestContextResolver
import com.aandiclub.tech.blog.presentation.image.ImageUploadService
import com.aandiclub.tech.blog.presentation.v2.image.dto.V2ImageUploadResponse
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange

@RestController
@RequestMapping("/v2/posts/images")
class V2ImageController(
	private val imageUploadService: ImageUploadService,
	private val requestContextResolver: AiV2RequestContextResolver,
) {
	@PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
	suspend fun upload(
		exchange: ServerWebExchange,
		@RequestPart("file") file: FilePart,
	): ResponseEntity<AiV2ApiResponse<V2ImageUploadResponse>> {
		requestContextResolver.resolve(exchange)
		return ResponseEntity.ok(AiV2ApiResponse.success(imageUploadService.upload(file).toV2()))
	}
}
