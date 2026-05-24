package com.example.financeapp.util

import com.example.financeapp.R

object AvatarManager {
    val predefinedAvatars = mapOf(
        "avatar_1" to R.drawable.ic_avatar_1,
        "avatar_2" to R.drawable.ic_avatar_2,
        "avatar_3" to R.drawable.ic_avatar_3,
        "avatar_4" to R.drawable.ic_avatar_4,
        "avatar_5" to R.drawable.ic_avatar_5
    )

    fun getAvatarResource(id: String?): Int {
        return predefinedAvatars[id] ?: R.drawable.ic_avatar_1
    }
}
