package com.example.lifegame.util

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.lifegame.data.entity.LogEntity
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LogExportHelper {

    fun exportLogsToCsv(context: Context, logs: List<LogEntity>): File? {
        if (logs.isEmpty()) {
            Toast.makeText(context, "没有日志可导出", Toast.LENGTH_SHORT).show()
            return null
        }

        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "LifeGame日志导出_$timestamp.csv"
            
            val cacheDir = File(context.cacheDir, "exports")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }
            
            val file = File(cacheDir, fileName)
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            
            OutputStreamWriter(FileOutputStream(file), "UTF-8").use { writer ->
                writer.write("\uFEFF")
                
                writer.write("序号,日志类型,标题,详细内容,时间,是否锁定\n")
                
                logs.sortedByDescending { it.timestamp }.forEachIndexed { index, log ->
                    val escapedTitle = escapeCsvField(log.title)
                    val escapedDetails = escapeCsvField(log.details)
                    val dateStr = dateFormat.format(Date(log.timestamp))
                    val lockedStr = if (log.isLocked) "是" else "否"
                    
                    writer.write("${index + 1},${log.type},$escapedTitle,$escapedDetails,$dateStr,$lockedStr\n")
                }
            }

            return file
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "导出失败: ${e.message}", Toast.LENGTH_SHORT).show()
            return null
        }
    }

    private fun escapeCsvField(field: String): String {
        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            return "\"${field.replace("\"", "\"\"")}\""
        }
        return field
    }

    fun shareFile(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "LifeGame日志导出")
                putExtra(Intent.EXTRA_TEXT, "这是从LifeGame应用导出的日志记录，请查收。")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooserIntent = Intent.createChooser(shareIntent, "分享日志文件")
            chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooserIntent)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "分享失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
