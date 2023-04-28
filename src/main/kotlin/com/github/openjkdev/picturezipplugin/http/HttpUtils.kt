package com.github.openjkdev.picturezipplugin.http

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.*
import java.net.HttpURLConnection
import java.net.URL


/**
 * 该示例中，我们首先定义了三个URL：fileUrl表示要上传的文件在本地的路径；uploadUrl表示文件上传的目标URL；downloadUrl表示文件下载的目标URL。
 *然后，我们使用HttpURLConnection打开一个连接到uploadUrl，设置请求方法为POST，
设置Content-Type为application/octet-stream，设置Content-Disposition为attachment，指定要上传的文件名。
接着，我们获取该连接的输出流和文件的输入流，每次读取BUFFER_SIZE大小的字节数据并写入输出流，直到读取完整个文件。
最后，我们关闭输入输出流并获取响应码，判断上传是否成功。

接下来，我们使用HttpURLConnection打开一个连接到downloadUrl，设置请求方法为GET。
然后，我们获取该连接的输入流和本地文件的输出流，每次读取BUFFER_SIZE大小的字节数据并写入输出流，直到读取完整个文件。
最后，我们关闭输入输出流并获取响应码，判断下载是否成功。
 */

object HttpUtils {
    private const val BUFFER_SIZE = 4096
    private const val TIME_OUT = 20000
    val uploadUrl = "https://tinypng.com/backend/opt/shrink"

    @JvmStatic
    fun main(args: Array<String>) {
        uploadFile(uploadUrl, "C:\\Users\\Administrator\\Downloads\\画规助手.png") {
            println(it)
        }
    }

    // Upload file
    fun uploadFile(host: String, path: String, callBack: (String) -> Unit) {
        try {
            val url = URL(host)
            val file = File(path)
            val conn = url.openConnection() as HttpURLConnection
            conn.doOutput = true
            conn.requestMethod = "POST"
            conn.connectTimeout = TIME_OUT
            conn.setRequestProperty("Content-Type", "image/png")
            conn.setRequestProperty("Content-Disposition", "attachment; filename=\"" + file.name + "\"")
            conn.setRequestProperty("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Safari/537.36")
            val outputStream = conn.outputStream
            val inputStream = FileInputStream(file)
            val buffer = ByteArray(BUFFER_SIZE)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }
            outputStream.flush()
            outputStream.close()
            inputStream.close()
            val responseCode = conn.responseCode
            var isTemp: InputStream? = null
            var br: BufferedReader? = null
            var result: String? = null // 返回结果字符串

            if (responseCode >= HttpURLConnection.HTTP_OK && responseCode < HttpURLConnection.HTTP_MULT_CHOICE) {
                isTemp = conn.inputStream
                br = BufferedReader(InputStreamReader(isTemp, "UTF-8"))
                // 存放数据
                // 存放数据
                val sbf = StringBuffer()
                var temp: String? = null
                while (br.readLine().also { temp = it } != null) {
                    sbf.append(temp)
                }
                result = sbf.toString()
                callBack.invoke(createResponseBody(0, result, "success"))
            } else {
                isTemp = conn.errorStream
                if (isTemp != null) {
                    br = BufferedReader(InputStreamReader(isTemp, "UTF-8"))
                    // 存放数据
                    // 存放数据
                    val sbf = StringBuffer()
                    var temp: String? = null
                    while (br.readLine().also { temp = it } != null) {
                        sbf.append(temp)
                    }
                    result = sbf.toString()
                    callBack.invoke(createResponseBody(-1, result, "fail"))
                } else {
                    callBack.invoke(createResponseBody(-1, "code $responseCode", "fail"))
                }

            }
            br?.close()
            isTemp?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    // Download file
    fun downFile(host: String, saveFile: String, callBack: (String) -> Unit) {
        try {
            val url = URL(host)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = TIME_OUT
            val inputStream = conn.inputStream
            val outputStream = FileOutputStream(saveFile)
            val buffer = ByteArray(BUFFER_SIZE)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }
            outputStream.close()
            inputStream.close()
            val responseCode = conn.responseCode

            if (responseCode >= HttpURLConnection.HTTP_OK && responseCode < HttpURLConnection.HTTP_MULT_CHOICE) {
                callBack.invoke(createResponseBody(0, "{}", "success"))
            } else {
                callBack.invoke(createResponseBody(-1, "{}", "fail code $responseCode"))
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun createResponseBody(code: Int, data: String, msg: String): String {
        return JsonObject().apply {
            addProperty("code", code)
            addProperty("msg", msg)
            add("data", JsonParser.parseString(data).asJsonObject)
        }.toString()
    }
}
