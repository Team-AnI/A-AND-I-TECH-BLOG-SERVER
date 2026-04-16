package com.aandiclub.tech.blog.common.error

object ErrorMessageLocalizer {
	fun localize(raw: String?): String {
		val reason = raw?.trim().orEmpty()
		if (reason.isBlank()) return "오류가 발생했습니다."

		return when {
			reason.startsWith("post not found") -> "게시글을 찾을 수 없습니다."
			reason == "draft posts are only available in draft list" -> "초안 글은 초안 목록에서만 조회할 수 있습니다."
			reason == "only post owner or collaborator can edit" -> "게시글 작성자나 공동 작성자만 수정할 수 있습니다."
			reason == "only post owner can modify collaborators" -> "게시글 작성자만 공동 작성자를 수정할 수 있습니다."
			reason == "only post owner can add collaborators" -> "게시글 작성자만 공동 작성자를 추가할 수 있습니다."
			reason == "primary author cannot be changed" -> "주 작성자는 변경할 수 없습니다."
			reason == "requester id does not match ownerId" -> "요청자 정보가 작성자와 일치하지 않습니다."
			reason == "requester id is required" -> "요청자 정보가 필요합니다."
			reason == "title is required" -> "제목은 필수입니다."
			reason == "contentMarkdown is required for published post" -> "게시하려면 본문 내용이 필요합니다."
			reason == "owner is already the primary author" -> "작성자는 이미 주 작성자입니다."
			reason == "collaborator nickname is required for new user" -> "신규 공동 작성자 닉네임이 필요합니다."
			reason == "s3 bucket is not configured" -> "이미지 저장소가 설정되지 않았습니다."
			reason == "missing content type" -> "콘텐츠 타입이 필요합니다."
			reason.startsWith("s3 upload failed") -> "이미지 업로드에 실패했습니다."
			reason == "s3 client configuration error" -> "스토리지 설정에 문제가 있습니다."
			reason.startsWith("unsupported content type") -> "지원하지 않는 콘텐츠 타입입니다."
			reason == "file size exceeds limit" -> "파일 크기 제한을 초과했습니다."
			reason == "invalid token" -> "유효하지 않은 인증 토큰입니다."
			reason == "user id claim not found" -> "사용자 정보를 확인할 수 없습니다."
			reason == "missing Authorization header" -> "Authorization 헤더가 필요합니다."
			reason == "invalid Authorization header" -> "유효한 Authorization 헤더가 필요합니다."
			reason == "missing bearer token" -> "Bearer 토큰이 필요합니다."
			reason == "jwt verifier is not configured" -> "인증 설정이 올바르지 않습니다."
			reason == "validation failed" -> "입력값이 올바르지 않습니다."
			reason == "bad request" -> "잘못된 요청입니다."
			reason == "internal server error" -> "서버 내부 오류가 발생했습니다."
			else -> reason
		}
	}
}
