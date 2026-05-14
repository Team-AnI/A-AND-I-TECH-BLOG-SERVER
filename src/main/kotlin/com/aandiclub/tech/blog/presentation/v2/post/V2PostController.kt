package com.aandiclub.tech.blog.presentation.v2.post

import com.aandiclub.tech.blog.common.api.v2.AiV2ApiResponse
import com.aandiclub.tech.blog.common.api.v2.AiV2ErrorCatalog
import com.aandiclub.tech.blog.common.api.v2.AiV2ErrorDescriptor
import com.aandiclub.tech.blog.common.api.v2.AiV2ProtocolException
import com.aandiclub.tech.blog.common.api.v2.AiV2RequestContextResolver
import com.aandiclub.tech.blog.common.logging.ApiLogContext
import com.aandiclub.tech.blog.common.logging.BlogEventType
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
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.validation.Valid
import jakarta.validation.Validator
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
import org.springframework.http.codec.multipart.FormFieldPart
import org.springframework.http.codec.multipart.Part
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
import org.springframework.web.server.ServerWebInputException
import org.springframework.web.server.ResponseStatusException
import java.nio.charset.StandardCharsets
import java.util.UUID

@Validated
@RestController
@RequestMapping("/v2/posts")
class V2PostController(
	private val postService: PostService,
	private val imageUploadService: ImageUploadService,
	private val requestContextResolver: AiV2RequestContextResolver,
	private val objectMapper: ObjectMapper,
	private val validator: Validator,
) {
	@PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
	suspend fun create(
		exchange: ServerWebExchange,
		@RequestPart("post") postPart: Part,
		@RequestPart("thumbnail", required = false) thumbnail: FilePart?,
	): ResponseEntity<AiV2ApiResponse<V2PostResponse>> {
		val request = parseCreateRequest(postPart)
		validateCreateRequest(request)
		requestContextResolver.resolveAuthenticated(exchange)
		val created = withBlogOperationFailure(AiV2ErrorCatalog.postCreateFailed) {
			val uploadedThumbnailUrl = thumbnail?.let { imageUploadService.upload(it).url }
			postService.create(request.copy(thumbnailUrl = uploadedThumbnailUrl ?: request.thumbnailUrl))
		}
		ApiLogContext.get(exchange)?.markEvent(BlogEventType.BLOG_POST_CREATED.name, created.id)
		return ResponseEntity.status(HttpStatus.CREATED).body(AiV2ApiResponse.success(created.toV2()))
	}

	@GetMapping("/{postId}")
	suspend fun get(
		exchange: ServerWebExchange,
		@PathVariable postId: UUID,
	): ResponseEntity<AiV2ApiResponse<V2PostResponse>> {
		requestContextResolver.resolvePublic(exchange)
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
		requestContextResolver.resolvePublic(exchange)
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
		val requestContext = requestContextResolver.resolveAuthenticated(exchange)
		return ResponseEntity.ok(
			AiV2ApiResponse.success(postService.listMyPosts(page, size, requestContext.requireRequesterId(), status, type).toV2()),
		)
	}

	@GetMapping("/drafts")
	suspend fun listDrafts(
		exchange: ServerWebExchange,
		@RequestParam(defaultValue = "0") @Min(0) page: Int,
		@RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
		@RequestParam(required = false) type: PostType?,
	): ResponseEntity<AiV2ApiResponse<V2PagedPostResponse>> {
		requestContextResolver.resolvePublic(exchange)
		return ResponseEntity.ok(AiV2ApiResponse.success(postService.listDrafts(page, size, type).toV2()))
	}

	@GetMapping("/drafts/me")
	suspend fun listMyDrafts(
		exchange: ServerWebExchange,
		@RequestParam(defaultValue = "0") @Min(0) page: Int,
		@RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
		@RequestParam(required = false) type: PostType?,
	): ResponseEntity<AiV2ApiResponse<V2PagedPostResponse>> {
		val requestContext = requestContextResolver.resolveAuthenticated(exchange)
		return ResponseEntity.ok(
			AiV2ApiResponse.success(postService.listMyDrafts(page, size, requestContext.requireRequesterId(), type).toV2()),
		)
	}

	@PatchMapping("/{postId}", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
	suspend fun patchMultipart(
		exchange: ServerWebExchange,
		@PathVariable postId: UUID,
		@Valid @RequestPart("post") request: PatchPostRequest,
		@RequestPart("thumbnail", required = false) thumbnail: FilePart?,
	): ResponseEntity<AiV2ApiResponse<V2PostResponse>> {
		val requestContext = requestContextResolver.resolveAuthenticated(exchange)
		val operation = resolvePatchOperation(request.status)
		val patched = withBlogOperationFailure(operation.failureDescriptor) {
			val uploadedThumbnailUrl = thumbnail?.let { imageUploadService.upload(it).url }
			postService.patch(
				postId,
				requestContext.requireRequesterId(),
				request.copy(thumbnailUrl = uploadedThumbnailUrl ?: request.thumbnailUrl),
			)
		}
		ApiLogContext.get(exchange)?.markEvent(operation.eventType.name, patched.id)
		return ResponseEntity.ok(AiV2ApiResponse.success(patched.toV2()))
	}

	@PatchMapping("/{postId}", consumes = [MediaType.APPLICATION_JSON_VALUE])
	suspend fun patchJson(
		exchange: ServerWebExchange,
		@PathVariable postId: UUID,
		@Valid @RequestBody request: PatchPostRequest,
	): ResponseEntity<AiV2ApiResponse<V2PostResponse>> {
		val requestContext = requestContextResolver.resolveAuthenticated(exchange)
		val operation = resolvePatchOperation(request.status)
		val patched = withBlogOperationFailure(operation.failureDescriptor) {
			postService.patch(postId, requestContext.requireRequesterId(), request)
		}
		ApiLogContext.get(exchange)?.markEvent(operation.eventType.name, patched.id)
		return ResponseEntity.ok(AiV2ApiResponse.success(patched.toV2()))
	}

	@PostMapping("/{postId}/collaborators")
	suspend fun addCollaborator(
		exchange: ServerWebExchange,
		@PathVariable postId: UUID,
		@Valid @RequestBody request: V2AddCollaboratorRequest,
	): ResponseEntity<AiV2ApiResponse<V2PostResponse>> {
		val requestContext = requestContextResolver.resolveAuthenticated(exchange)
		val added = withBlogOperationFailure(AiV2ErrorCatalog.postUpdateFailed) {
			postService.addCollaborator(
				postId,
				requestContext.requireRequesterId(),
				AddCollaboratorRequest(
					ownerId = null,
					collaborator = request.collaborator,
				),
			)
		}
		ApiLogContext.get(exchange)?.markEvent(BlogEventType.BLOG_POST_UPDATED.name, added.id)
		return ResponseEntity.ok(AiV2ApiResponse.success(added.toV2()))
	}

	@DeleteMapping("/{postId}")
	suspend fun delete(
		exchange: ServerWebExchange,
		@PathVariable postId: UUID,
	): ResponseEntity<AiV2ApiResponse<V2DeletePostResponse>> {
		requestContextResolver.resolveAuthenticated(exchange)
		withBlogOperationFailure(AiV2ErrorCatalog.postDeleteFailed) {
			postService.delete(postId)
		}
		ApiLogContext.get(exchange)?.markEvent(BlogEventType.BLOG_POST_DELETED.name, postId)
		return ResponseEntity.ok(AiV2ApiResponse.success(V2DeletePostResponse(deleted = true)))
	}

	private data class BlogPatchOperation(
		val eventType: BlogEventType,
		val failureDescriptor: AiV2ErrorDescriptor,
	)

	private fun resolvePatchOperation(status: PostStatus?): BlogPatchOperation =
		when (status) {
			PostStatus.Published -> BlogPatchOperation(BlogEventType.BLOG_POST_PUBLISHED, AiV2ErrorCatalog.postPublishFailed)
			PostStatus.Draft -> BlogPatchOperation(BlogEventType.BLOG_POST_UNPUBLISHED, AiV2ErrorCatalog.postUnpublishFailed)
			else -> BlogPatchOperation(BlogEventType.BLOG_POST_UPDATED, AiV2ErrorCatalog.postUpdateFailed)
		}

	private suspend fun <T> withBlogOperationFailure(
		descriptor: AiV2ErrorDescriptor,
		block: suspend () -> T,
	): T =
		try {
			block()
		} catch (exception: AiV2ProtocolException) {
			throw exception
		} catch (exception: ResponseStatusException) {
			if (exception.statusCode.is5xxServerError) {
				throw AiV2ProtocolException(descriptor = descriptor, cause = exception)
			}
			throw exception
		} catch (exception: IllegalArgumentException) {
			throw exception
		} catch (exception: Exception) {
			throw AiV2ProtocolException(descriptor = descriptor, cause = exception)
		}

	private suspend fun parseCreateRequest(postPart: Part): CreatePostRequest {
		val payload = when (postPart) {
			is FormFieldPart -> postPart.value()
			else -> {
				val joined = DataBufferUtils.join(postPart.content()).awaitSingle()
				try {
					val bytes = ByteArray(joined.readableByteCount())
					joined.read(bytes)
					String(bytes, StandardCharsets.UTF_8)
				} finally {
					DataBufferUtils.release(joined)
				}
			}
		}

		return try {
			objectMapper.readValue(payload, CreatePostRequest::class.java)
		} catch (exception: JsonProcessingException) {
			throw ServerWebInputException("Invalid post part", null, exception)
		}
	}

	private fun validateCreateRequest(request: CreatePostRequest) {
		val violations = validator.validate(request)
		if (violations.isEmpty()) {
			return
		}

		val message = violations
			.sortedBy { it.propertyPath.toString() }
			.joinToString("; ") { "${it.propertyPath}: ${it.message}" }
		throw IllegalArgumentException(message)
	}
}
