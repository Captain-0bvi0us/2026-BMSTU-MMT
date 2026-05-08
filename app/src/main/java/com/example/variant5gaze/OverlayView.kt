package com.example.variant5gaze

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.sin

class OverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val faceBoxPaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    private val eyePaint = Paint().apply {
        color = Color.YELLOW
        style = Paint.Style.FILL
    }

    private val gazePaint = Paint().apply {
        color = Color.CYAN
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }

    private val axisXPaint = Paint().apply {
        color = Color.RED
        strokeWidth = 5f
    }

    private val axisYPaint = Paint().apply {
        color = Color.GREEN
        strokeWidth = 5f
    }

    private val axisZPaint = Paint().apply {
        color = Color.BLUE
        strokeWidth = 5f
    }

    private var result: FacePoseResult? = null
    private var isFrontCamera = true

    fun update(result: FacePoseResult?, isFrontCamera: Boolean) {
        this.result = result
        this.isFrontCamera = isFrontCamera
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val pose = result ?: return

        val leftEye = mapToView(pose.leftEyeCenter, pose.imageWidth, pose.imageHeight)
        val rightEye = mapToView(pose.rightEyeCenter, pose.imageWidth, pose.imageHeight)
        val faceCenter = mapToView(pose.faceCenter, pose.imageWidth, pose.imageHeight)
        val gazeEnd = mapToView(pose.gazeEndPoint2D, pose.imageWidth, pose.imageHeight)

        val box = RectF(
            mapX(pose.boundingBox.left.toFloat(), pose.imageWidth),
            mapY(pose.boundingBox.top.toFloat(), pose.imageHeight),
            mapX(pose.boundingBox.right.toFloat(), pose.imageWidth),
            mapY(pose.boundingBox.bottom.toFloat(), pose.imageHeight)
        )
        box.sort()
        canvas.drawRect(box, faceBoxPaint)

        canvas.drawCircle(leftEye.x, leftEye.y, 8f, eyePaint)
        canvas.drawCircle(rightEye.x, rightEye.y, 8f, eyePaint)

        val gazeStart = PointF((leftEye.x + rightEye.x) / 2f, (leftEye.y + rightEye.y) / 2f)
        canvas.drawLine(gazeStart.x, gazeStart.y, gazeEnd.x, gazeEnd.y, gazePaint)
        drawArrowHead(canvas, gazeStart, gazeEnd, gazePaint)

        drawHeadAxes(canvas, faceCenter, pose.eulerX, pose.eulerY, pose.eulerZ)
    }

    private fun drawHeadAxes(canvas: Canvas, origin: PointF, pitchX: Float, yawY: Float, rollZ: Float) {
        val scale = 130f
        val rz = Math.toRadians(rollZ.toDouble()).toFloat()
        val ry = Math.toRadians(yawY.toDouble()).toFloat()
        val rx = Math.toRadians(pitchX.toDouble()).toFloat()

        val xAxis = PointF(
            origin.x + scale * cos(rz) * cos(ry),
            origin.y + scale * sin(rz) * cos(ry)
        )
        val yAxis = PointF(
            origin.x + scale * (-sin(rz) * cos(rx)),
            origin.y + scale * (cos(rz) * cos(rx))
        )
        val zAxis = PointF(
            origin.x + scale * sin(ry),
            origin.y - scale * sin(rx)
        )

        canvas.drawLine(origin.x, origin.y, xAxis.x, xAxis.y, axisXPaint)
        canvas.drawLine(origin.x, origin.y, yAxis.x, yAxis.y, axisYPaint)
        canvas.drawLine(origin.x, origin.y, zAxis.x, zAxis.y, axisZPaint)
    }

    private fun drawArrowHead(canvas: Canvas, start: PointF, end: PointF, paint: Paint) {
        val dx = end.x - start.x
        val dy = end.y - start.y
        val len = kotlin.math.sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
        val ux = dx / len
        val uy = dy / len
        val size = 24f

        val p1 = PointF(end.x - ux * size - uy * size * 0.5f, end.y - uy * size + ux * size * 0.5f)
        val p2 = PointF(end.x - ux * size + uy * size * 0.5f, end.y - uy * size - ux * size * 0.5f)
        canvas.drawLine(end.x, end.y, p1.x, p1.y, paint)
        canvas.drawLine(end.x, end.y, p2.x, p2.y, paint)
    }

    private fun mapToView(point: PointF, imageWidth: Int, imageHeight: Int): PointF {
        return PointF(
            mapX(point.x, imageWidth),
            mapY(point.y, imageHeight)
        )
    }

    private fun mapX(x: Float, imageWidth: Int): Float {
        val scaled = x * width / imageWidth.toFloat()
        return if (isFrontCamera) width - scaled else scaled
    }

    private fun mapY(y: Float, imageHeight: Int): Float {
        return y * height / imageHeight.toFloat()
    }
}
