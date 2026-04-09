package com.aandiclub.tech.blog.presentation.v2.image

import com.aandiclub.tech.blog.presentation.image.dto.ImageUploadResponse
import com.aandiclub.tech.blog.presentation.v2.image.dto.V2ImageUploadResponse

internal fun ImageUploadResponse.toV2(): V2ImageUploadResponse =
	V2ImageUploadResponse(
		url = url,
		key = key,
		contentType = contentType,
		size = size,
	)
