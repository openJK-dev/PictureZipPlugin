package com.github.openjkdev.picturezipplugin.utils

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.math.BigDecimal

object FileUtils {
    /**
     * 获取文件夹大小
     */
    fun getFolderSize(file: File): Long {
        var size: Long = 0
        try {
            size = file.length()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return size
    }

    /**
     * 格式化单位
     */
    fun getFormatSize(size: Long): String {
        val kiloByte = size / 1024
        if (kiloByte < 1) {
            return "${size}B"
        }
        val megaByte = kiloByte / 1024
        if (megaByte < 1) {
            val result1 = BigDecimal(kiloByte)
            return result1.setScale(2, BigDecimal.ROUND_HALF_UP)
                    .toPlainString() + "KB"
        }
        val gigaByte = megaByte / 1024
        if (gigaByte < 1) {
            val result2 = BigDecimal(megaByte)
            return result2.setScale(2, BigDecimal.ROUND_HALF_UP)
                    .toPlainString() + "MB"
        }
        val teraBytes = gigaByte / 1024
        if (teraBytes < 1) {
            val result3 = BigDecimal(gigaByte)
            return result3.setScale(2, BigDecimal.ROUND_HALF_UP)
                    .toPlainString() + "GB"
        }
        val result4 = BigDecimal(teraBytes)
        return (result4.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString()
                + "TB")
    }

    fun fileIsImage(path: String): Boolean {
        if (path.isEmpty() || (!path.endsWith(".png", true)
                        && !path.endsWith(".jpg", true)
                        && !path.endsWith(".jpeg", true))) {
            return false
        }
        return true
    }

    /**
     * 读取文件内容
     */
    fun readFile(filePath: String): String {
        val file = File(filePath)
        if (!file.exists()) {
            throw IllegalArgumentException("File $filePath does not exist")
        }
        return file.readText()
    }

    /**
     * 写入文件内容
     */
    fun writeFile(filePath: String, content: String) {
        val file = File(filePath)
        file.writeText(content)
    }

    /**
     * 删除文件
     */
    fun deleteFile(filePath: String) {
        val file = File(filePath)
        file.delete()
    }

    fun copyFile(sourceFile: File, destinationFile: File) {
        val input = FileInputStream(sourceFile)
        val output = FileOutputStream(destinationFile)
        val buffer = ByteArray(1024)
        var length: Int
        while (input.read(buffer).also { length = it } > 0) {
            output.write(buffer, 0, length)
        }
        input.close()
        output.close()
    }

}