package com.aandiclub.tech.blog.presentation.post

import com.aandiclub.tech.blog.common.api.ApiResponse
import com.aandiclub.tech.blog.domain.post.PostStatus
import com.aandiclub.tech.blog.presentation.image.ImageUploadService
import com.aandiclub.tech.blog.presentation.post.dto.CreatePostRequest
import com.aandiclub.tech.blog.presentation.post.dto.PagedPostResponse
import com.aandiclub.tech.blog.presentation.post.dto.PatchPostRequest
import com.aandiclub.tech.blog.presentation.post.dto.PostResponse
import com.aandiclub.tech.blog.presentation.post.service.PostService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@Validated
@RestController
@RequestMapping("/v1/posts")
@Tag(name = "Posts", description = "Post CRUD API")
class PostController(
	private val postService: PostService,
	private val imageUploadService: ImageUploadService,
) {
	@PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
	@Operation(summary = "Create post (multipart, optional thumbnail upload)")
	@ApiResponses(
		value = [
			SwaggerApiResponse(responseCode = "201", description = "Created", content = [Content(schema = Schema(implementation = PostResponse::class))]),
			SwaggerApiResponse(responseCode = "400", description = "Validation failed"),
		],
	)
	suspend fun create(
		@Valid @RequestPart("post") request: CreatePostRequest,
		@RequestPart("thumbnail", required = false) thumbnail: FilePart?,
	): ResponseEntity<ApiResponse<PostResponse>> {
		val uploadedThumbnailUrl = thumbnail?.let { imageUploadService.upload(it).url }
		return ResponseEntity.status(201).body(
			ApiResponse.success(
				postService.create(request.copy(thumbnailUrl = uploadedThumbnailUrl ?: request.thumbnailUrl)),
			),
		)
	}

	@GetMapping("/{postId}")
	@Operation(summary = "Get post detail")
	@ApiResponses(
		value = [
			SwaggerApiResponse(responseCode = "200", description = "OK", content = [Content(schema = Schema(implementation = PostResponse::class))]),
			SwaggerApiResponse(responseCode = "404", description = "Not found"),
		],
	)
	suspend fun get(@PathVariable postId: UUID): ResponseEntity<ApiResponse<PostResponse>> =
		ResponseEntity.ok(ApiResponse.success(postService.get(postId)))

	@GetMapping
	@Operation(summary = "List posts")
	@ApiResponses(
		value = [
			SwaggerApiResponse(responseCode = "200", description = "OK", content = [Content(schema = Schema(implementation = PagedPostResponse::class))]),
		],
	)
	suspend fun list(
		@RequestParam(defaultValue = "0") @Min(0) page: Int,
		@RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
		@RequestParam(required = false) status: PostStatus?,
	): ResponseEntity<ApiResponse<PagedPostResponse>> =
		ResponseEntity.ok(ApiResponse.success(postService.list(page, size, status)))

	@GetMapping("/drafts")
	@Operation(summary = "List draft posts")
	@ApiResponses(
		value = [
			SwaggerApiResponse(responseCode = "200", description = "OK", content = [Content(schema = Schema(implementation = PagedPostResponse::class))]),
		],
	)
	suspend fun listDrafts(
		@RequestParam(defaultValue = "0") @Min(0) page: Int,
		@RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
	): ResponseEntity<ApiResponse<PagedPostResponse>> =
		ResponseEntity.ok(ApiResponse.success(postService.listDrafts(page, size)))

	@PatchMapping("/{postId}")
	@Operation(summary = "Patch post")
	@ApiResponses(
		value = [
			SwaggerApiResponse(responseCode = "200", description = "OK", content = [Content(schema = Schema(implementation = PostResponse::class))]),
			SwaggerApiResponse(responseCode = "404", description = "Not found"),
		],
	)
	suspend fun patch(
		@PathVariable postId: UUID,
		@Valid @RequestBody request: PatchPostRequest,
	): ResponseEntity<ApiResponse<PostResponse>> =
		ResponseEntity.ok(ApiResponse.success(postService.patch(postId, request)))

	@DeleteMapping("/{postId}")
	@Operation(summary = "Delete post")
	@ApiResponses(
		value = [
			SwaggerApiResponse(responseCode = "200", description = "OK"),
			SwaggerApiResponse(responseCode = "404", description = "Not found"),
		],
	)
	suspend fun delete(@PathVariable postId: UUID): ResponseEntity<ApiResponse<Map<String, Boolean>>> {
		postService.delete(postId)
		return ResponseEntity.ok(ApiResponse.success(mapOf("deleted" to true)))
	}
}
