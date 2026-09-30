package com.egoal.darkestpixeldungeon.update

import kotlinx.serialization.Serializable

@Serializable
data class UpdateInfo(
        val versionName: String = "",
        val versionCode: Int = 0,
        val desc: String = "",
        val downloadUrl: String = "")
