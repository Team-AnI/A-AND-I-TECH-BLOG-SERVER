package com.aandiclub.tech.blog.common.auth

interface AuthTokenService {
	suspend fun extractUserId(authorizationHeader: String?): String
}
