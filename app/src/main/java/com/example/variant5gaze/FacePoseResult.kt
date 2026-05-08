package com.example.variant5gaze

import android.graphics.PointF
import android.graphics.Rect

data class FacePoseResult(
    val boundingBox: Rect,
    val leftEyeCenter: PointF,
    val rightEyeCenter: PointF,
    val faceCenter: PointF,
    val imageWidth: Int,
    val imageHeight: Int,
    val eulerX: Float,
    val eulerY: Float,
    val eulerZ: Float,
    val gazeVector3D: FloatArray,
    val gazeEndPoint2D: PointF,
    val glassesLikely: Boolean
)
