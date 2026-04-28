package com.aandiclub.tech.blog.presentation.v2.post

import com.aandiclub.tech.blog.common.api.v2.AiV2ApiResponse
import com.aandiclub.tech.blog.common.api.v2.AiV2RequestContextResolver
import com.aandiclub.tech.blog.domain.post.PostStatus
import com.aandiclub.tech.blog.domain.post.PostType
import com.aandiclub.tech.blog.presentation.post.service.PostService
import com.aandiclub.tech.blog.presentation.v2.post.dto.V2PagedPostResponse
import com.aandiclub.tech.blog.presentation.v2.post.dto.V2PostResponse
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.server.ServerWebExchange
import java.util.UUID

@Validated
@RestController
@RequestMapping("/v2/blogs")
class V2BlogQueryController(
	private val postService: PostService,
	private val requestContextResolver: AiV2RequestContextResolver,
) {
	@GetMapping("/{postId}")
	suspend fun get(
		exchange: ServerWebExchange,
		@PathVariable postId: UUID,
	): ResponseEntity<AiV2ApiResponse<V2PostResponse>> {
		requestContextResolver.resolvePublic(exchange)
		return ResponseEntity.ok(AiV2ApiResponse.success(requireBlog(postService.get(postId)).toV2()))
	}

	@GetMapping
	suspend fun list(
		exchange: ServerWebExchange,
		@RequestParam(defaultValue = "0") @Min(0) page: Int,
		@RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
		@RequestParam(required = false) status: PostStatus?,
	): ResponseEntity<AiV2ApiResponse<V2PagedPostResponse>> {
		requestContextResolver.resolvePublic(exchange)
		return ResponseEntity.ok(AiV2ApiResponse.success(postService.list(page, size, status, PostType.Blog).toV2()))
	}

	@GetMapping("/me")
	suspend fun listMyBlogs(
		exchange: ServerWebExchange,
		@RequestParam(defaultValue = "0") @Min(0) page: Int,
		@RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
		@RequestParam(required = false) status: PostStatus?,
	): ResponseEntity<AiV2ApiResponse<V2PagedPostResponse>> {
		val requestContext = requestContextResolver.resolveAuthenticated(exchange)
		return ResponseEntity.ok(
			AiV2ApiResponse.success(postService.listMyPosts(page, size, requestContext.requireRequesterId(), status, PostType.Blog).toV2()),
		)
	}

	@GetMapping("/drafts")
	suspend fun listDrafts(
		exchange: ServerWebExchange,
		@RequestParam(defaultValue = "0") @Min(0) page: Int,
		@RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
	): ResponseEntity<AiV2ApiResponse<V2PagedPostResponse>> {
		requestContextResolver.resolvePublic(exchange)
		return ResponseEntity.ok(AiV2ApiResponse.success(postService.listDrafts(page, size, PostType.Blog).toV2()))
	}

	@GetMapping("/drafts/me")
	suspend fun listMyDrafts(
		exchange: ServerWebExchange,
		@RequestParam(defaultValue = "0") @Min(0) page: Int,
		@RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
	): ResponseEntity<AiV2ApiResponse<V2PagedPostResponse>> {
		val requestContext = requestContextResolver.resolveAuthenticated(exchange)
		return ResponseEntity.ok(
			AiV2ApiResponse.success(postService.listMyDrafts(page, size, requestContext.requireRequesterId(), PostType.Blog).toV2()),
		)
	}

	@GetMapping("/scheduled/me")
	suspend fun listMyScheduledPosts(
		exchange: ServerWebExchange,
		@RequestParam(defaultValue = "0") @Min(0) page: Int,
		@RequestParam(defaultValue = "20") @Min(1) @Max(100) size: Int,
	): ResponseEntity<AiV2ApiResponse<V2PagedPostResponse>> {
		val requestContext = requestContextResolver.resolveAuthenticated(exchange)
		return ResponseEntity.ok(
			AiV2ApiResponse.success(postService.listMyScheduledPosts(page, size, requestContext.requireRequesterId(), PostType.Blog).toV2()),
		)
	}

	private fun requireBlog(post: com.aandiclub.tech.blog.presentation.post.dto.PostResponse): com.aandiclub.tech.blog.presentation.post.dto.PostResponse {
		if (post.type != PostType.Blog) {
			throw ResponseStatusException(HttpStatus.NOT_FOUND, "blog post not found")
		}
		return post
	}
}
