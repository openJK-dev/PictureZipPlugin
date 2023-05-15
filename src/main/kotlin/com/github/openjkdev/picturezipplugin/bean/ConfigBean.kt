package com.github.openjkdev.picturezipplugin.bean

import kotlinx.serialization.Serializable

@Serializable
data class ConfigBean(
        var enableTranslate: Boolean? = false,
        val translateKey: String? = "",
        val translateAppId:String? = "",
        val savePath: List<ImagePathBean>? = null,
        val prefixList: List<PreFixBean>? = null
)
