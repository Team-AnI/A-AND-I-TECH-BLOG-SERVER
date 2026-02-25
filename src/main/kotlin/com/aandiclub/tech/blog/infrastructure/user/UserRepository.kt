package com.aandiclub.tech.blog.infrastructure.user

import com.aandiclub.tech.blog.domain.user.User
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface UserRepository : CoroutineCrudRepository<User, String>
