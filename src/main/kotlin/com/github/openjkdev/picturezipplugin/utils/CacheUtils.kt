package com.github.openjkdev.picturezipplugin.utils

import com.github.openjkdev.picturezipplugin.bean.ConfigBean
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId
import java.io.File

object CacheUtils {
    const val CONFIG_NAME = "config.json"
    const val CACHE_NAME = "pictureZipPlugin"
    const val TEMP_IMAGE = "temp.png"
    var PLUGIN_PATH: String = PluginManagerCore
            .getPlugin(PluginId.findId("com.github.openjkdev.picturezipplugin"))
            ?.pluginPath?.parent
            ?.toAbsolutePath().toString()
    var CONFIG_PATH = PLUGIN_PATH + File.separator + CACHE_NAME + File.separator + CONFIG_NAME
    var TEMP_IMAGE_PATH = PLUGIN_PATH + File.separator + CACHE_NAME + File.separator + TEMP_IMAGE

    init {
        resetConfig()
    }

    /**
     * 重置配置
     */
    private fun resetConfig() {
        val file = File(CONFIG_PATH)
        if (!file.exists()) {
            file.parentFile.mkdirs()
            file.createNewFile()
            FileUtils.writeFile(file.absolutePath, """{
                |"enableTranslate":false,
                |"translateKey":"key",
                |"translateAppId":"appId",
                |"savePath":[
                |{"name":"路径别名1",
                |"path":"路径绝对路径1",
                |"select":true},
                |{"name":"路径别名2",
                |"path":"路径绝对路径2",
                |"select":false}
                |],
                |"prefixList":[
                |{"preName":"前缀别名1",
                |"select":true,
                |"value":"前缀1"},
                |{"preName":"前缀别名2",
                |"select":false,
                |"value":"前缀2"},
                |{"preName":"前缀别名3",
                |"select":false,
                |"value":"前缀3"}
                |]
                |}""".trimMargin())
        }
    }

    /**
     * 读取配置文件信息
     */
    fun readConfig(needFormat: Boolean = true): String {
        val file = File(CONFIG_PATH)
        if (!file.exists()) {
            resetConfig()
        }
        val data = FileUtils.readFile(file.absolutePath).replace(if (needFormat) "\n" else "", "")
        return if (data.isEmpty() || !JsonParser.parseString(data).isJsonObject) {
            resetConfig()
            FileUtils.readFile(file.absolutePath).replace(if (needFormat) "\n" else "", "")
        } else {
            data
        }
    }

    /**
     * 写入配置文件
     */
    fun writeConfig(content: String) {
        val file = File(CONFIG_PATH)
        FileUtils.deleteFile(CONFIG_PATH)
        file.parentFile.mkdirs()
        file.createNewFile()
        FileUtils.writeFile(file.absolutePath, content)
    }

    /**
     * 是否是错误配置
     */
    fun isErrorConfig(data: String?): Boolean {
        return data.isNullOrEmpty() || !JsonParser.parseString(data).isJsonObject
    }

    /**
     * 解析配置类
     */
    fun parseConfig(content: String): ConfigBean {
        val gson = Gson()
        return gson.fromJson(content, ConfigBean::class.java)
    }

    fun configToJsonStr(configBean: ConfigBean): String {
        return configBean.toString()
    }

}