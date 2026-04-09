package com.aandiclub.tech.blog.presentation.post.service

import com.aandiclub.tech.blog.domain.post.Post
import com.aandiclub.tech.blog.domain.post.PostCollaborator
import com.aandiclub.tech.blog.domain.post.PostStatus
import com.aandiclub.tech.blog.domain.post.PostType
import com.aandiclub.tech.blog.domain.user.User
import com.aandiclub.tech.blog.infrastructure.post.PostCollaboratorRepository
import com.aandiclub.tech.blog.infrastructure.post.PostRepository
import com.aandiclub.tech.blog.infrastructure.user.UserRepository
import com.aandiclub.tech.blog.presentation.post.dto.AddCollaboratorRequest
import com.aandiclub.tech.blog.presentation.post.dto.CreatePostRequest
import com.aandiclub.tech.blog.presentation.post.dto.PostAuthorRequest
import com.aandiclub.tech.blog.presentation.post.dto.PostAuthorResponse
import com.aandiclub.tech.blog.presentation.post.dto.PagedPostResponse
import com.aandiclub.tech.blog.presentation.post.dto.PatchPostRequest
import com.aandiclub.tech.blog.presentation.post.dto.PostResponse
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.flow.toList
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.r2dbc.core.R2dbcEntityOperations
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.util.UUID
import kotlin.math.ceil

@Service
class PostServiceImpl(
	private val postRepository: PostRepository,
	private val postCollaboratorRepository: PostCollaboratorRepository,
	private val userRepository: UserRepository,
	private val entityOperations: R2dbcEntityOperations,
) : PostService {

	override suspend fun create(request: CreatePostRequest): PostResponse {
		validatePostByStatus(request.title, request.contentMarkdown, request.status)
		val upsertedAuthor = upsertAuthorFromRequest(request.author)
		val post = Post(
			title = request.title,
			summary = request.summary ?: "",
			contentMarkdown = request.contentMarkdown,
			thumbnailUrl = request.thumbnailUrl,
			authorId = request.author.id,
			type = request.type ?: PostType.Blog,
			status = request.status,
		)
		val saved = entityOperations.insert(post).awaitSingle()
		mergeCollaborators(saved.id, saved.authorId, request.collaborators.orEmpty())
		val collaborators = loadCollaborators(saved.id)
		return saved.toResponse(
			author = upsertedAuthor?.toAuthorResponse() ?: resolveAuthor(request.author.id, request.author.nickname, request.author.profileImageUrl),
			collaborators = collaborators,
		)
	}

	override suspend fun get(postId: UUID): PostResponse {
		val post = postRepository.findByIdAndStatusNot(postId, PostStatus.Deleted) ?: throw notFound(postId)
		return buildPostResponse(post)
	}

	override suspend fun list(page: Int, size: Int, status: PostStatus?, type: PostType?): PagedPostResponse {
		if (status == PostStatus.Draft) {
			throw ResponseStatusException(HttpStatus.BAD_REQUEST, "draft posts are only available in draft list")
		}
		return listByStatus(page, size, status ?: PostStatus.Published, type ?: PostType.Blog)
	}

	override suspend fun listMyPosts(page: Int, size: Int, requesterId: String, status: PostStatus?, type: PostType?): PagedPostResponse {
		if (status == PostStatus.Draft) {
			throw ResponseStatusException(HttpStatus.BAD_REQUEST, "draft posts are only available in draft list")
		}
		val actorId = normalizeRequesterId(requesterId)
		return listByStatusAndUser(page, size, status ?: PostStatus.Published, type ?: PostType.Blog, actorId)
	}

	override suspend fun listDrafts(page: Int, size: Int, type: PostType?): PagedPostResponse =
		listByStatus(page, size, PostStatus.Draft, type ?: PostType.Blog)

	override suspend fun listMyDrafts(page: Int, size: Int, requesterId: String, type: PostType?): PagedPostResponse {
		val actorId = normalizeRequesterId(requesterId)
		return listByStatusAndUser(page, size, PostStatus.Draft, type ?: PostType.Blog, actorId)
	}

	private suspend fun listByStatus(page: Int, size: Int, status: PostStatus, type: PostType): PagedPostResponse {
		val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
		val posts = postRepository.findByStatusAndType(status, type, pageable).toList()
		val usersById = loadUsersById(posts.map { it.authorId }.toSet())
		val collaboratorsByPostId = loadCollaboratorsByPostId(posts.map { it.id }.toSet())
		val items = posts.map { post ->
			post.toResponse(
				author = usersById[post.authorId]?.toAuthorResponse() ?: fallbackAuthor(post.authorId),
				collaborators = collaboratorsByPostId[post.id] ?: emptyList(),
			)
		}
		val totalElements = postRepository.countByStatusAndType(status, type)
		val totalPages = if (totalElements == 0L) 0 else ceil(totalElements.toDouble() / size.toDouble()).toInt()

		return PagedPostResponse(
			items = items,
			page = page,
			size = size,
			totalElements = totalElements,
			totalPages = totalPages,
		)
	}

	private suspend fun listByStatusAndUser(page: Int, size: Int, status: PostStatus, type: PostType, userId: String): PagedPostResponse {
		val offset = page.toLong() * size.toLong()
		val posts = postRepository.findByStatusAndTypeAndUser(status, type, userId, size, offset).toList()
		val usersById = loadUsersById(posts.map { it.authorId }.toSet())
		val collaboratorsByPostId = loadCollaboratorsByPostId(posts.map { it.id }.toSet())
		val items = posts.map { post ->
			post.toResponse(
				author = usersById[post.authorId]?.toAuthorResponse() ?: fallbackAuthor(post.authorId),
				collaborators = collaboratorsByPostId[post.id] ?: emptyList(),
			)
		}
		val totalElements = postRepository.countByStatusAndTypeAndUser(status, type, userId)
		val totalPages = if (totalElements == 0L) 0 else ceil(totalElements.toDouble() / size.toDouble()).toInt()

		return PagedPostResponse(
			items = items,
			page = page,
			size = size,
			totalElements = totalElements,
			totalPages = totalPages,
		)
	}

	override suspend fun patch(postId: UUID, requesterId: String, request: PatchPostRequest): PostResponse {
		val current = postRepository.findByIdAndStatusNot(postId, PostStatus.Deleted) ?: throw notFound(postId)
		val actorId = normalizeRequesterId(requesterId)
		if (!canEdit(current.id, current.authorId, actorId)) {
			throw ResponseStatusException(HttpStatus.FORBIDDEN, "only post owner or collaborator can edit")
		}
		if (request.collaborators != null && actorId != current.authorId) {
			if (hasCollaboratorChanges(postId, request.collaborators)) {
				throw ResponseStatusException(HttpStatus.FORBIDDEN, "only post owner can modify collaborators")
			}
		}
		if (request.author != null && request.author.id != current.authorId) {
			throw ResponseStatusException(HttpStatus.BAD_REQUEST, "primary author cannot be changed")
		}
		val upsertedAuthor = request.author?.let { upsertAuthorFromRequest(it) }
		val nextTitle = request.title ?: current.title
		val nextSummary = request.summary ?: ""
		val nextContentMarkdown = request.contentMarkdown ?: current.contentMarkdown
		val nextType = request.type ?: current.type
		val nextStatus = request.status ?: current.status
		validatePostByStatus(nextTitle, nextContentMarkdown, nextStatus)
		val updated = current.copy(
			title = nextTitle,
			summary = nextSummary,
			contentMarkdown = nextContentMarkdown,
			thumbnailUrl = request.thumbnailUrl ?: current.thumbnailUrl,
			authorId = current.authorId,
			type = nextType,
			status = nextStatus,
			updatedAt = Instant.now(),
		)
		val saved = postRepository.save(updated)
		if (request.collaborators != null && actorId == current.authorId) {
			val collaborators = request.collaborators
			syncCollaborators(saved.id, saved.authorId, collaborators)
		}
		val collaborators = loadCollaborators(saved.id)
		return saved.toResponse(
			author = upsertedAuthor?.toAuthorResponse()
				?: resolveAuthor(saved.authorId, request.author?.nickname, request.author?.profileImageUrl),
			collaborators = collaborators,
		)
	}

	override suspend fun addCollaborator(postId: UUID, requesterId: String, request: AddCollaboratorRequest): PostResponse {
		val post = postRepository.findByIdAndStatusNot(postId, PostStatus.Deleted) ?: throw notFound(postId)
		val actorId = normalizeRequesterId(requesterId)
		request.ownerId?.trim()?.takeIf { it.isNotEmpty() }?.let { claimedOwnerId ->
			if (claimedOwnerId != actorId) {
				throw ResponseStatusException(HttpStatus.FORBIDDEN, "requester id does not match ownerId")
			}
		}
		if (post.authorId != actorId) {
			throw ResponseStatusException(HttpStatus.FORBIDDEN, "only post owner can add collaborators")
		}
		mergeCollaborators(postId, post.authorId, listOf(request.collaborator))
		return buildPostResponse(post)
	}

	private suspend fun canEdit(postId: UUID, ownerId: String, actorId: String): Boolean {
		if (actorId == ownerId) return true
		return postCollaboratorRepository.existsByPostIdAndUserId(postId, actorId)
	}

	private suspend fun hasCollaboratorChanges(postId: UUID, collaborators: List<PostAuthorRequest>): Boolean {
		val requestedIds = collaborators.map { it.id.trim() }.toSet()
		val existingIds = postCollaboratorRepository.findByPostId(postId).toList().map { it.userId }.toSet()
		return requestedIds != existingIds
	}

	private fun normalizeRequesterId(requesterId: String): String {
		val actorId = requesterId.trim()
		if (actorId.isBlank()) {
			throw ResponseStatusException(HttpStatus.BAD_REQUEST, "requester id is required")
		}
		return actorId
	}

	private fun validatePostByStatus(title: String, contentMarkdown: String, status: PostStatus) {
		if (title.isBlank()) {
			throw ResponseStatusException(HttpStatus.BAD_REQUEST, "title is required")
		}
		if (status == PostStatus.Published && contentMarkdown.isBlank()) {
			throw ResponseStatusException(HttpStatus.BAD_REQUEST, "contentMarkdown is required for published post")
		}
	}

	private suspend fun mergeCollaborators(postId: UUID, ownerId: String, collaborators: List<PostAuthorRequest>) {
		val targetIds = normalizeCollaboratorIds(ownerId, collaborators)
		if (targetIds.isEmpty()) return
		for (collaboratorId in targetIds) {
			if (!postCollaboratorRepository.existsByPostIdAndUserId(postId, collaboratorId)) {
				entityOperations.insert(
					PostCollaborator(
						postId = postId,
						userId = collaboratorId,
					),
				).awaitSingle()
			}
		}
	}

	private suspend fun syncCollaborators(postId: UUID, ownerId: String, collaborators: List<PostAuthorRequest>) {
		val targetIds = normalizeCollaboratorIds(ownerId, collaborators)
		val existingRows = postCollaboratorRepository.findByPostId(postId).toList()
		val existingByUserId = existingRows.associateBy { it.userId }
		val existingIds = existingByUserId.keys

		val toAdd = targetIds - existingIds
		val toRemove = existingIds - targetIds

		for (collaboratorId in toAdd) {
			entityOperations.insert(
				PostCollaborator(
					postId = postId,
					userId = collaboratorId,
				),
			).awaitSingle()
		}

		for (collaboratorId in toRemove) {
			val row = existingByUserId[collaboratorId] ?: continue
			postCollaboratorRepository.delete(row)
		}
	}

	private suspend fun normalizeCollaboratorIds(ownerId: String, collaborators: List<PostAuthorRequest>): Set<String> {
		if (collaborators.isEmpty()) return emptySet()
		val uniqueCollaborators = collaborators.associateBy { it.id }.values
		val ids = linkedSetOf<String>()
		for (collaborator in uniqueCollaborators) {
			val collaboratorId = collaborator.id
			if (collaboratorId == ownerId) {
				throw ResponseStatusException(HttpStatus.BAD_REQUEST, "owner is already the primary author")
			}
			if (upsertAuthorFromRequest(collaborator) == null) {
				throw ResponseStatusException(HttpStatus.BAD_REQUEST, "collaborator nickname is required for new user")
			}
			ids += collaboratorId
		}
		return ids
	}

	override suspend fun delete(postId: UUID) {
		val current = postRepository.findByIdAndStatusNot(postId, PostStatus.Deleted) ?: throw notFound(postId)
		postRepository.save(
			current.copy(
				status = PostStatus.Deleted,
				updatedAt = Instant.now(),
			),
		)
	}

	private fun notFound(postId: UUID): ResponseStatusException =
		ResponseStatusException(HttpStatus.NOT_FOUND, "post not found: $postId")

	private suspend fun loadUsersById(authorIds: Set<String>): Map<String, User> {
		if (authorIds.isEmpty()) return emptyMap()
		return userRepository.findAllById(authorIds).toList().associateBy { it.id }
	}

	private suspend fun upsertAuthorFromRequest(author: PostAuthorRequest): User? {
		val existing = userRepository.findById(author.id)
		val nickname = author.nickname?.takeIf { it.isNotBlank() } ?: existing?.nickname
		val thumbnailUrl = author.profileImageUrl ?: existing?.thumbnailUrl

		// Legacy payload(authorId string) can miss nickname; skip upsert in that case.
		if (nickname == null) return existing

		if (existing == null) {
			return entityOperations.insert(
				User(
					id = author.id,
					nickname = nickname,
					thumbnailUrl = thumbnailUrl,
				),
			).awaitSingle()
		}

		if (existing.nickname == nickname && existing.thumbnailUrl == thumbnailUrl) {
			return existing
		}

		return userRepository.save(
			existing.copy(
				nickname = nickname,
				thumbnailUrl = thumbnailUrl,
				updatedAt = Instant.now(),
			),
		)
	}

	private suspend fun loadCollaborators(postId: UUID): List<PostAuthorResponse> {
		val collaborators = postCollaboratorRepository.findByPostId(postId).toList()
		if (collaborators.isEmpty()) return emptyList()
		val usersById = loadUsersById(collaborators.map { it.userId }.toSet())
		return collaborators.map { collaborator ->
			usersById[collaborator.userId]?.toAuthorResponse() ?: fallbackAuthor(collaborator.userId)
		}
	}

	private suspend fun loadCollaboratorsByPostId(postIds: Set<UUID>): Map<UUID, List<PostAuthorResponse>> {
		if (postIds.isEmpty()) return emptyMap()
		val collaborators = postCollaboratorRepository.findByPostIdIn(postIds).toList()
		if (collaborators.isEmpty()) return emptyMap()
		val usersById = loadUsersById(collaborators.map { it.userId }.toSet())
		return collaborators.groupBy { it.postId }.mapValues { (_, values) ->
			values.map { collaborator ->
				usersById[collaborator.userId]?.toAuthorResponse() ?: fallbackAuthor(collaborator.userId)
			}
		}
	}

	private suspend fun buildPostResponse(post: Post): PostResponse {
		val author = resolveAuthor(post.authorId)
		val collaborators = loadCollaborators(post.id)
		return post.toResponse(
			author = author,
			collaborators = collaborators,
		)
	}

	private suspend fun resolveAuthor(
		authorId: String,
		fallbackNickname: String? = null,
		fallbackThumbnailUrl: String? = null,
	): PostAuthorResponse =
		userRepository.findById(authorId)?.toAuthorResponse()
			?: fallbackAuthor(authorId, fallbackNickname, fallbackThumbnailUrl)

	private fun User.toAuthorResponse() = PostAuthorResponse(
		id = id,
		nickname = nickname,
		profileImageUrl = thumbnailUrl,
	)

	private fun fallbackAuthor(
		authorId: String,
		nickname: String? = null,
		thumbnailUrl: String? = null,
	) = PostAuthorResponse(
		id = authorId,
		nickname = nickname?.takeIf { it.isNotBlank() } ?: "unknown",
		profileImageUrl = thumbnailUrl,
	)

	private fun Post.toResponse(
		author: PostAuthorResponse,
		collaborators: List<PostAuthorResponse> = emptyList(),
	) = PostResponse(
		id = id,
		title = title,
		summary = summary,
		contentMarkdown = contentMarkdown,
		thumbnailUrl = thumbnailUrl,
		author = author,
		collaborators = collaborators,
		type = type,
		status = status,
		createdAt = createdAt,
		updatedAt = updatedAt,
	)
}
