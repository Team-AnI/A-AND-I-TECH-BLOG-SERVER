package com.aandiclub.tech.blog.presentation.image

import com.aandiclub.tech.blog.common.api.ApiResponse
import com.aandiclub.tech.blog.presentation.image.dto.ImageUploadResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/posts/images")
@Tag(name = "Images", description = "Image upload API")
class ImageController(
	private val imageUploadService: ImageUploadService,
) {
	@PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
	@Operation(summary = "Upload image and return public metadata")
	@ApiResponses(
		value = [
			SwaggerApiResponse(responseCode = "200", description = "Upload success", content = [Content(schema = Schema(implementation = ImageUploadResponse::class))]),
			SwaggerApiResponse(responseCode = "413", description = "Payload too large"),
			SwaggerApiResponse(responseCode = "415", description = "Unsupported media type"),
		],
	)
	suspend fun upload(
		@RequestPart("file") file: FilePart,
	): ResponseEntity<ApiResponse<ImageUploadResponse>> = ResponseEntity.ok(
		ApiResponse.success(imageUploadService.upload(file)),
	)
}
