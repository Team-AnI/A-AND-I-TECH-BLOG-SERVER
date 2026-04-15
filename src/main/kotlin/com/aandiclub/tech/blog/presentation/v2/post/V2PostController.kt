package com.aandiclub.tech.blog.presentation.v2.post

import com.aandiclub.tech.blog.common.api.v2.AiV2ApiResponse
import com.aandiclub.tech.blog.common.api.v2.AiV2RequestContextResolver
import com.aandiclub.tech.blog.domain.post.PostStatus
import com.aandiclub.tech.blog.domain.post.PostType
import com.aandiclub.tech.blog.presentation.image.ImageUploadService
import com.aandiclub.tech.blog.presentation.post.dto.AddCollaboratorRequest
import com.aandiclub.tech.blog.presentation.post.dto.CreatePostRequest
import com.aandiclub.tech.blog.presentation.post.dto.PatchPostRequest
import com.aandiclub.tech.blog.presentation.post.service.PostService
import com.aandiclub.tech.blog.presentation.v2.post.dto.V2AddCollaboratorRequest
import com.aandiclub.tech.blog.presentation.v2.post.dto.V2DeletePostResponse
import com.aandiclub.tech.blog.presentation.v2.post.dto.V2PagedPostResponse
import com.aandiclub.tech.blog.presentation.v2.post.dto.V2PostResponse
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.http.HttpStatus
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
import org.springframework.web.server.ServerWebExchange
import java.util.UUID

@Validated
@RestController
@RequestMapping("/v2/posts")
class V2PostController(
	private val postService: PostService,
	private val imageUploadService: ImageUploadService,
	private val requestContextResolver: AiV2RequestContextResolver,
) {
	@PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
	suspend fun create(
		exchange: ServerWebExchange,
		@Valid @RequestPart("post") request: CreatePostRequest,
		@RequestPart("thumbnail", required = false) thumbnail: FilePart?,
	): ResponseEntity<AiV2ApiResponse<V2PostResponse>> {
		requestContextResolver.resolve(exchange)
		val uploadedThumbnailUrl = thumbnail?.let { imageUploadService.upload(it).url }
		val created = postService.create(request.copy(thumbnailUrl = uploadedThumbnailUrl ?: request.thumbnailUrl))
		return ResponseEntity.status(HttpStatus.CREATED).body(AiV2ApiResponse.success(created.toV2()))
	}

	@GetMapping("/{postId}")
	suspend fun get(
		exchange: ServerWebExchange,
		@PathVariable postId: UUID,
	): ResponseEntity<AiV2ApiResponse<V2PostResponse>> {
		requestContextResolver.resolve(exchange)
		return ResponseEntity.ok(AiV2ApiResponse.success(postService.get(postId).toV2()))
	}

	@GetMapping
	suspend fun list(
		exchange: ServerWebExchange,
		@RequestParam(defaultValue = "0") @Min(0) page: Int,
		@RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
		@RequestParam(required = false) status: PostStatus?,
		@RequestParam(required = false) type: PostType?,
	): ResponseEntity<AiV2ApiResponse<V2PagedPostResponse>> {
		requestContextResolver.resolve(exchange)
		return ResponseEntity.ok(AiV2ApiResponse.success(postService.list(page, size, status, type).toV2()))
	}

	@GetMapping("/me")
	suspend fun listMyPosts(
		exchange: ServerWebExchange,
		@RequestParam(defaultValue = "0") @Min(0) page: Int,
		@RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
		@RequestParam(required = false) status: PostStatus?,
		@RequestParam(required = false) type: PostType?,
	): ResponseEntity<AiV2ApiResponse<V2PagedPostResponse>> {
		val requestContext = requestContextResolver.resolve(exchange)
		return ResponseEntity.ok(
			AiV2ApiResponse.success(postService.listMyPosts(page, size, requestContext.requesterId, status, type).toV2()),
		)
	}

	@GetMapping("/drafts")
	suspend fun listDrafts(
		exchange: ServerWebExchange,
		@RequestParam(defaultValue = "0") @Min(0) page: Int,
		@RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
		@RequestParam(required = false) type: PostType?,
	): ResponseEntity<AiV2ApiResponse<V2PagedPostResponse>> {
		requestContextResolver.resolve(exchange)
		return ResponseEntity.ok(AiV2ApiResponse.success(postService.listDrafts(page, size, type).toV2()))
	}

	@GetMapping("/drafts/me")
	suspend fun listMyDrafts(
		exchange: ServerWebExchange,
		@RequestParam(defaultValue = "0") @Min(0) page: Int,
		@RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
		@RequestParam(required = false) type: PostType?,
	): ResponseEntity<AiV2ApiResponse<V2PagedPostResponse>> {
		val requestContext = requestContextResolver.resolve(exchange)
		return ResponseEntity.ok(
			AiV2ApiResponse.success(postService.listMyDrafts(page, size, requestContext.requesterId, type).toV2()),
		)
	}

	@PatchMapping("/{postId}", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
	suspend fun patchMultipart(
		exchange: ServerWebExchange,
		@PathVariable postId: UUID,
		@Valid @RequestPart("post") request: PatchPostRequest,
		@RequestPart("thumbnail", required = false) thumbnail: FilePart?,
	): ResponseEntity<AiV2ApiResponse<V2PostResponse>> {
		val requestContext = requestContextResolver.resolve(exchange)
		val uploadedThumbnailUrl = thumbnail?.let { imageUploadService.upload(it).url }
		val patched = postService.patch(
			postId,
			requestContext.requesterId,
			request.copy(thumbnailUrl = uploadedThumbnailUrl ?: request.thumbnailUrl),
		)
		return ResponseEntity.ok(AiV2ApiResponse.success(patched.toV2()))
	}

	@PatchMapping("/{postId}", consumes = [MediaType.APPLICATION_JSON_VALUE])
	suspend fun patchJson(
		exchange: ServerWebExchange,
		@PathVariable postId: UUID,
		@Valid @RequestBody request: PatchPostRequest,
	): ResponseEntity<AiV2ApiResponse<V2PostResponse>> {
		val requestContext = requestContextResolver.resolve(exchange)
		return ResponseEntity.ok(
			AiV2ApiResponse.success(postService.patch(postId, requestContext.requesterId, request).toV2()),
		)
	}

	@PostMapping("/{postId}/collaborators")
	suspend fun addCollaborator(
		exchange: ServerWebExchange,
		@PathVariable postId: UUID,
		@Valid @RequestBody request: V2AddCollaboratorRequest,
	): ResponseEntity<AiV2ApiResponse<V2PostResponse>> {
		val requestContext = requestContextResolver.resolve(exchange)
		val added = postService.addCollaborator(
			postId,
			requestContext.requesterId,
			AddCollaboratorRequest(
				ownerId = null,
				collaborator = request.collaborator,
			),
		)
		return ResponseEntity.ok(AiV2ApiResponse.success(added.toV2()))
	}

	@DeleteMapping("/{postId}")
	suspend fun delete(
		exchange: ServerWebExchange,
		@PathVariable postId: UUID,
	): ResponseEntity<AiV2ApiResponse<V2DeletePostResponse>> {
		requestContextResolver.resolve(exchange)
		postService.delete(postId)
		return ResponseEntity.ok(AiV2ApiResponse.success(V2DeletePostResponse(deleted = true)))
	}
}
