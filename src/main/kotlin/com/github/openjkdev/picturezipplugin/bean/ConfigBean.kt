package com.github.openjkdev.picturezipplugin.bean

import kotlinx.serialization.Serializable

@Serializable
data class ConfigBean(
        var enableTranslate: Boolean? = false,
        val translateKey: String? = "",
        val translateAppId:String? = "",
        val savePath: List<ImagePathBean>? = null,
        val prefixList: List<PreFixBean>? = null
){
    override fun toString(): String {
        val savePathStr = savePath?.map{it.toString()}?.joinToString(",") { it }
        val prefixListStr = prefixList?.map{it.toString()}?.joinToString(",") { it }

        return """{
            |"enableTranslate":"$enableTranslate",
            |"translateKey":"$translateKey", 
            |"translateAppId":"$translateAppId", 
            |"savePath":[$savePathStr], 
            |"prefixList":[$prefixListStr]
            |}""".trimMargin()
    }
}
