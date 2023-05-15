package com.github.openjkdev.picturezipplugin.services

import com.github.openjkdev.picturezipplugin.utils.MD5Utils
import com.google.gson.Gson
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.kotlin.idea.util.ifFalse
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class BaiduTranslateService(var appId: String, var appKey: String) {
    //Step1. 拼接字符串1：
    //拼接appid=2015063000000001+q=apple+salt=1435660288+密钥=12345678得到字符串1：“2015063000000001apple143566028812345678”
    //Step2. 计算签名：（对字符串1做MD5加密）
    //sign=MD5(2015063000000001apple143566028812345678)，得到sign=f89f9594663708c1605f3d736d01d2d4
    private val API_URL = "http://api.fanyi.baidu.com/api/trans/vip/translate"

    @Throws(Exception::class)
    fun translate(text: String, from: String, to: String): String? {
        val salt = System.currentTimeMillis()
        val signTemp = appId + text + salt + appKey
        val sign = MD5Utils.encrypt(signTemp)
        val urlStr: String = API_URL + "?q=" + URLEncoder.encode(text, "UTF-8") + "&from=" + from + "&to=" + to + "&appid=" + appId + "&salt=" + salt + "&sign=" + sign
        val url = URL(urlStr)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        val `in` = BufferedReader(InputStreamReader(conn.inputStream))
        var inputLine: String?
        val response = StringBuffer()
        while (`in`.readLine().also { inputLine = it } != null) {
            response.append(inputLine)
        }
        `in`.close()
        return response.toString()
    }

    /**
     * 解析翻译结果
     */
    fun parseResult(result: String?): String? {
        if (result == null) {
            return null
        }

        val translateList = Json.parseToJsonElement(result).jsonObject["trans_result"]?.jsonArray
        var dst: String? = null
        if (translateList != null && translateList.size>0) {
            dst = translateList[0].jsonObject["dst"]?.jsonPrimitive?.content
        }
        if (dst != null) {
            dst = dst.replace(" ", "_").lowercase()
        }
        return dst
    }
}