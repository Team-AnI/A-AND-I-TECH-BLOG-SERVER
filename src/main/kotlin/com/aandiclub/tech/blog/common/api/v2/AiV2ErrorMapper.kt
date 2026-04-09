package com.aandiclub.tech.blog.common.api.v2

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException

@Component
class AiV2ErrorMapper {
	fun map(exception: ResponseStatusException): AiV2ErrorDescriptor {
		val status = HttpStatus.resolve(exception.statusCode.value())
		val reason = exception.reason?.trim().orEmpty()

		return when {
			status == HttpStatus.NOT_FOUND && reason.startsWith("post not found") -> AiV2ErrorCatalog.postNotFound
			status == HttpStatus.BAD_REQUEST && reason == "draft posts are only available in draft list" -> AiV2ErrorCatalog.draftListOnly
			status == HttpStatus.FORBIDDEN && reason == "only post owner or collaborator can edit" -> AiV2ErrorCatalog.postEditForbidden
			status == HttpStatus.FORBIDDEN && reason == "only post owner can modify collaborators" -> AiV2ErrorCatalog.collaboratorEditForbidden
			status == HttpStatus.FORBIDDEN && reason == "only post owner can add collaborators" -> AiV2ErrorCatalog.addCollaboratorForbidden
			status == HttpStatus.BAD_REQUEST && reason == "primary author cannot be changed" -> AiV2ErrorCatalog.primaryAuthorImmutable
			status == HttpStatus.BAD_REQUEST && reason == "title is required" -> AiV2ErrorCatalog.titleRequired
			status == HttpStatus.BAD_REQUEST && reason == "contentMarkdown is required for published post" -> AiV2ErrorCatalog.contentRequiredForPublished
			status == HttpStatus.BAD_REQUEST && reason == "owner is already the primary author" -> AiV2ErrorCatalog.ownerAlreadyPrimaryAuthor
			status == HttpStatus.BAD_REQUEST && reason == "collaborator nickname is required for new user" -> AiV2ErrorCatalog.collaboratorNicknameRequired
			status == HttpStatus.UNAUTHORIZED -> AiV2ErrorCatalog.invalidAuthenticate
			status == HttpStatus.BAD_GATEWAY -> AiV2ErrorCatalog.s3UploadFailed
			status == HttpStatus.SERVICE_UNAVAILABLE -> AiV2ErrorCatalog.externalSystemUnavailable
			status == HttpStatus.FORBIDDEN -> AiV2ErrorCatalog.forbidden
			status == HttpStatus.BAD_REQUEST -> AiV2ErrorCatalog.badRequest
			else -> AiV2ErrorCatalog.internalServerError
		}
	}
}
