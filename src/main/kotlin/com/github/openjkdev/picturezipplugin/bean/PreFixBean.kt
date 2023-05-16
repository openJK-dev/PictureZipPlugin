package com.github.openjkdev.picturezipplugin.bean

import kotlinx.serialization.Serializable

@Serializable
class PreFixBean(var preName: String, var select: Boolean, var value: String) {
    override fun toString(): String {
        return """{
            |"preName":"$preName", 
            |"select":"$select", 
            |"value":"$value"
            |}""".trimMargin()
    }
}