package com.gglee.meihuayishu.export

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import androidx.core.content.FileProvider
import com.gglee.meihuayishu.ai.AiPrompt
import com.gglee.meihuayishu.engine.MeihuaChart
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ChartImageExporter {

    private val ink = 0xFF1F241F.toInt()
    private val paper = 0xFFF7F3E8.toInt()
    private val wash = 0xFFEDE6D6.toInt()
    private val pine = 0xFF335C4D.toInt()
    private val cinnabar = 0xFF943328.toInt()
    private val muted = 0xFF6B665C.toInt()

    fun renderBitmap(chart: MeihuaChart, width: Int = 1080): Bitmap {
        val pad = 48f
        val lineGap = 44f
        val sections = buildLines(chart)
        val height = (pad * 2 + sections.size * lineGap + 280).toInt().coerceAtLeast(1400)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(paper)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ink
            textSize = 56f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = muted
            textSize = 28f
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ink
            textSize = 30f
        }
        val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = pine
            textSize = 32f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val warnPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = cinnabar
            textSize = 28f
        }

        var y = pad + 56f
        canvas.drawText("梅花易数 · 解盘", pad, y, titlePaint)
        y += 40f
        canvas.drawText("仅供学习参考 · 可交其他 AI 解读", pad, y, subPaint)
        y += 28f
        canvas.drawRect(pad, y, width - pad, y + 3f, Paint().apply { color = wash })
        y += 48f

        // Hexagram glyphs
        y = drawHexBlock(canvas, chart, pad, y, width)
        y += 36f

        for (line in sections) {
            val paint = when (line.style) {
                LineStyle.ACCENT -> accentPaint
                LineStyle.WARN -> warnPaint
                LineStyle.MUTED -> subPaint
                LineStyle.BODY -> bodyPaint
            }
            val wrapped = wrap(line.text, paint, width - pad * 2)
            for (w in wrapped) {
                canvas.drawText(w, pad, y, paint)
                y += lineGap * 0.85f
            }
            y += 8f
        }

        y += 24f
        canvas.drawText("导出时间 ${formatTime(Date())}", pad, y, subPaint)
        return bitmap
    }

    private fun drawHexBlock(
        canvas: Canvas,
        chart: MeihuaChart,
        pad: Float,
        startY: Float,
        width: Int,
    ): Float {
        val colW = (width - pad * 2) / 3f
        val labels = listOf("本卦", "互卦", "变卦")
        val hexs = listOf(chart.original, chart.mutual, chart.changed)
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = pine
            textSize = 28f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ink
            textSize = 26f
            textAlign = Paint.Align.CENTER
        }
        val yangPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ink
            strokeWidth = 10f
            style = Paint.Style.STROKE
        }
        val yinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ink
            strokeWidth = 10f
            style = Paint.Style.STROKE
        }
        val movePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = cinnabar
            textSize = 22f
            textAlign = Paint.Align.LEFT
        }

        var maxY = startY
        hexs.forEachIndexed { idx, hex ->
            val cx = pad + colW * idx + colW / 2f
            var y = startY
            canvas.drawText(labels[idx], cx, y, labelPaint)
            y += 36f
            canvas.drawText(hex.name, cx, y, namePaint)
            y += 28f
            // draw from top (上爻) to bottom visually
            val lines = hex.lines
            for (i in 5 downTo 0) {
                val yang = lines[i]
                val left = cx - 70f
                val right = cx + 70f
                if (yang) {
                    canvas.drawLine(left, y, right, y, yangPaint)
                } else {
                    canvas.drawLine(left, y, cx - 12f, y, yinPaint)
                    canvas.drawLine(cx + 12f, y, right, y, yinPaint)
                }
                if (idx == 0 && i + 1 == chart.movingLine) {
                    canvas.drawText("动", right + 12f, y + 8f, movePaint)
                }
                y += 28f
            }
            canvas.drawText(
                "${hex.upper.symbol}${hex.lower.symbol}",
                cx,
                y + 8f,
                namePaint,
            )
            maxY = maxOf(maxY, y + 40f)
        }
        return maxY
    }

    private enum class LineStyle { BODY, ACCENT, MUTED, WARN }

    private data class TextLine(val text: String, val style: LineStyle)

    private fun buildLines(chart: MeihuaChart): List<TextLine> {
        val list = mutableListOf<TextLine>()
        list += TextLine("所问：${if (chart.hasQuestion) chart.question else "（未填写）"}", LineStyle.ACCENT)
        list += TextLine("起卦：${chart.method.label}", LineStyle.BODY)
        list += TextLine(chart.methodDetail, LineStyle.MUTED)
        if (chart.ganzhiLine.isNotBlank()) {
            list += TextLine("干支：${chart.ganzhiLine}", LineStyle.BODY)
        }
        list += TextLine(
            "体 ${chart.ti.displayName}（${chart.ti.element.hanzi}） · 用 ${chart.yong.displayName}（${chart.yong.element.hanzi}）",
            LineStyle.ACCENT,
        )
        list += TextLine(
            "体用：${chart.tiYongRelation.label} — ${chart.tiYongRelation.hint}",
            when (chart.tiYongRelation.name) {
                "YONG_KE_TI", "TI_SHENG_YONG" -> LineStyle.WARN
                else -> LineStyle.BODY
            },
        )
        list += TextLine("本卦 ${chart.original.title}", LineStyle.BODY)
        list += TextLine("互卦 ${chart.mutual.title}", LineStyle.BODY)
        list += TextLine("变卦 ${chart.changed.title}", LineStyle.BODY)
        list += TextLine("动爻：第${chart.movingLine}爻", LineStyle.BODY)
        chart.interpretations.take(3).forEach {
            list += TextLine("【${it.title}】", LineStyle.ACCENT)
            list += TextLine(it.detail, LineStyle.BODY)
        }
        return list
    }

    private fun wrap(text: String, paint: Paint, maxWidth: Float): List<String> {
        if (paint.measureText(text) <= maxWidth) return listOf(text)
        val result = mutableListOf<String>()
        var current = ""
        for (ch in text) {
            val trial = current + ch
            if (paint.measureText(trial) > maxWidth) {
                if (current.isNotEmpty()) result += current
                current = ch.toString()
            } else {
                current = trial
            }
        }
        if (current.isNotEmpty()) result += current
        return result
    }

    private fun formatTime(date: Date): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(date)

    fun saveToCache(context: Context, chart: MeihuaChart): File {
        val dir = File(context.cacheDir, "shared_images").apply { mkdirs() }
        val file = File(dir, "meihua_${chart.id}.png")
        val bitmap = renderBitmap(chart)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        bitmap.recycle()
        return file
    }

    fun shareImageAndPrompt(context: Context, chart: MeihuaChart) {
        val file = saveToCache(context, chart)
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        val prompt = AiPrompt.exportPromptForExternalAi(chart)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, prompt)
            putExtra(Intent.EXTRA_SUBJECT, "梅花易数解盘 · ${chart.summaryTitle}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "分享解盘图片给其他 AI"))
    }

    fun copyPrompt(context: Context, chart: MeihuaChart) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText(
            "梅花易数 AI 提示词",
            AiPrompt.exportPromptForExternalAi(chart),
        )
        cm.setPrimaryClip(clip)
    }
}
