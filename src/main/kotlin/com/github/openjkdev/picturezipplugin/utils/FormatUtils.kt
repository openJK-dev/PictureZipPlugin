package com.github.openjkdev.picturezipplugin.utils

import org.apache.http.util.TextUtils
import java.text.DecimalFormat
import java.text.Format


object FormatUtils {
    fun formatAmount2(amountStr: String): String {
        if (TextUtils.isEmpty(amountStr)) {
            return "0"
        }
        var result = amountStr
        val fm: Format = DecimalFormat("0.##")
        try {
            val number = result.toDouble()
            result = fm.format(number)
        } catch (ignore: Exception) {
        }
        return result
    }
}