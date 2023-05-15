package com.github.openjkdev.picturezipplugin.utils

import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

/**
 * MD5 加密
 */
object MD5Utils {
    fun encrypt(input: String): String? {
        try {
            // 创建 MessageDigest 实例并指定加密算法为 MD5
            val md = MessageDigest.getInstance("MD5")
            // 将输入字符串转换为字节数组并进行加密
            val encryptedBytes = md.digest(input.toByteArray())
            // 将加密后的字节数组转换为十六进制字符串
            val sb = StringBuilder()
            for (b in encryptedBytes) {
                sb.append(String.format("%02x", b))
            }
            return sb.toString()
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        }
        return null
    }
}