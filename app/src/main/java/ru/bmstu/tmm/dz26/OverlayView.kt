package ru.bmstu.tmm.dz26

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import kotlin.math.hypot
import kotlin.math.min

/**
 * Отрисовка осей головы (из матрицы вращения) и проекции вектора взгляда на плоскость кадра.
 */
class OverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    data class Frame(
        val yawDeg: Float,
        val pitchDeg: Float,
        val rollDeg: Float,
        val gazeCamera: FloatArray,
        val noseNormX: Float,
        val noseNormY: Float,
        val rotation4x4ColumnMajor: FloatArray?
    )

    @Volatile
    private var frame: Frame? = null

    var mirrorX: Boolean = false

    private val paintAxisX = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.RED
        strokeWidth = 6f
        style = Paint.Style.STROKE
    }
    private val paintAxisY = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.GREEN
        strokeWidth = 6f
        style = Paint.Style.STROKE
    }
    private val paintAxisZ = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(66, 165, 245)
        strokeWidth = 6f
        style = Paint.Style.STROKE
    }
    private val paintGaze = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.YELLOW
        strokeWidth = 8f
        style = Paint.Style.STROKE
    }
    private val paintGazeHead = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.MAGENTA
        strokeWidth = 5f
        style = Paint.Style.STROKE
    }
    private val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.YELLOW
        style = Paint.Style.FILL
    }

    fun updateFrame(f: Frame?) {
        frame = f
        postInvalidateOnAnimation()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val f = frame ?: return
        if (width == 0 || height == 0) return

        val nx = if (mirrorX) 1f - f.noseNormX else f.noseNormX
        val cx = nx * width
        val cy = f.noseNormY * height
        val scale = min(width, height) * 0.18f

        val R = f.rotation4x4ColumnMajor
        if (R != null && R.size >= 16) {
            // MediaPipe: 4×4 **column-major**, верхняя 3×3. Столбец c: (R[0,c], R[1,c]) ≈ (m[c*4], m[c*4+1])
            fun colXY(c: Int): Pair<Float, Float> =
                R[c * 4].toFloat() to R[c * 4 + 1].toFloat()
            val (exx, exy) = colXY(0)
            val (eyx, eyy) = colXY(1)
            val (ezx, ezy) = colXY(2)
            canvas.drawLine(cx, cy, cx + exx * scale, cy + exy * scale, paintAxisX)
            canvas.drawLine(cx, cy, cx + eyx * scale, cy + eyy * scale, paintAxisY)
            canvas.drawLine(cx, cy, cx + ezx * scale, cy + ezy * scale, paintAxisZ)
        }

        val g = f.gazeCamera
        if (g.size >= 3) {
            val gx = g[0]
            val gy = g[1]
            // Вектор взгляда в СК камеры: проецируем на плоскость изображения (X,Y)
            val len = hypot(gx.toDouble(), gy.toDouble()).toFloat().coerceAtLeast(1e-4f)
            val tx = cx + (gx / len) * scale * 1.6f
            val ty = cy + (gy / len) * scale * 1.6f
            canvas.drawLine(cx, cy, tx, ty, paintGaze)
            drawArrowHead(canvas, cx, cy, tx, ty)

            // «Основа» взгляда — направление оси Z головы (куда смотрит голова без учёта радужки)
            if (R != null && R.size >= 16) {
                val hzx = R[8].toFloat()
                val hzy = R[9].toFloat()
                val hlen = hypot(hzx.toDouble(), hzy.toDouble()).toFloat().coerceAtLeast(1e-4f)
                val hx = cx + (hzx / hlen) * scale * 1.2f
                val hy = cy + (hzy / hlen) * scale * 1.2f
                canvas.drawLine(cx, cy, hx, hy, paintGazeHead)
            }
        }
    }

    private fun drawArrowHead(canvas: Canvas, x0: Float, y0: Float, x1: Float, y1: Float) {
        val ang = kotlin.math.atan2((y1 - y0).toDouble(), (x1 - x0).toDouble())
        val head = min(width, height) * 0.025f
        val a1 = ang + Math.PI / 7
        val a2 = ang - Math.PI / 7
        val path = Path().apply {
            moveTo(x1, y1)
            lineTo((x1 - head * kotlin.math.cos(a1)).toFloat(), (y1 - head * kotlin.math.sin(a1)).toFloat())
            lineTo((x1 - head * kotlin.math.cos(a2)).toFloat(), (y1 - head * kotlin.math.sin(a2)).toFloat())
            close()
        }
        canvas.drawPath(path, arrowPaint)
    }
}
