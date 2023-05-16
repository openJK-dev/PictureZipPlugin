package com.github.openjkdev.picturezipplugin.bean

import kotlinx.serialization.Serializable

@Serializable
data class ImagePathBean(var name: String?, var path: String?, var select: Boolean) {
    override fun toString(): String {

        return """{
            |"name":"$name", 
            |"path":"${path?.replace("\\", "\\\\")}", 
            |"select":"$select"
            |}""".trimMargin()
    }
}