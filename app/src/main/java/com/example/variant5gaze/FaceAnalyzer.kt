package com.example.variant5gaze

import android.graphics.PointF
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceContour
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs

class FaceAnalyzer(
    private val onPoseReady: (FacePoseResult?) -> Unit
) : ImageAnalysis.Analyzer {

    private val detectorOptions = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
        .enableTracking()
        .build()

    private val detector: FaceDetector = FaceDetection.getClient(detectorOptions)
    private val isBusy = AtomicBoolean(false)

    override fun analyze(imageProxy: ImageProxy) {
        if (isBusy.getAndSet(true)) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            isBusy.set(false)
            return
        }

        val rotation = imageProxy.imageInfo.rotationDegrees
        val inputImage = InputImage.fromMediaImage(mediaImage, rotation)
        val resultWidth = if (rotation == 90 || rotation == 270) imageProxy.height else imageProxy.width
        val resultHeight = if (rotation == 90 || rotation == 270) imageProxy.width else imageProxy.height
        detector.process(inputImage)
            .addOnSuccessListener { faces ->
                onPoseReady(buildResult(faces, resultWidth, resultHeight))
            }
            .addOnFailureListener {
                onPoseReady(null)
            }
            .addOnCompleteListener {
                imageProxy.close()
                isBusy.set(false)
            }
    }

    private fun buildResult(faces: List<Face>, imageWidth: Int, imageHeight: Int): FacePoseResult? {
        val face = faces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() } ?: return null

        val leftEyePoints = face.getContour(FaceContour.LEFT_EYE)?.points ?: emptyList()
        val rightEyePoints = face.getContour(FaceContour.RIGHT_EYE)?.points ?: emptyList()
        if (leftEyePoints.isEmpty() || rightEyePoints.isEmpty()) return null

        val leftCenter = averagePoint(leftEyePoints)
        val rightCenter = averagePoint(rightEyePoints)
        val faceCenter = PointF(
            (face.boundingBox.left + face.boundingBox.right) / 2f,
            (face.boundingBox.top + face.boundingBox.bottom) / 2f
        )

        val noseBridge = face.getContour(FaceContour.NOSE_BRIDGE)?.points ?: emptyList()
        val noseTip = if (noseBridge.isNotEmpty()) averagePoint(noseBridge) else faceCenter

        val eyeDistance = (rightCenter.x - leftCenter.x).coerceAtLeast(1f)
        val eyesMiddle = PointF((leftCenter.x + rightCenter.x) / 2f, (leftCenter.y + rightCenter.y) / 2f)

        val horizontalEyeOffset = ((noseTip.x - eyesMiddle.x) / eyeDistance).coerceIn(-0.35f, 0.35f)
        val verticalEyeOffset = ((eyesMiddle.y - noseTip.y) / eyeDistance).coerceIn(-0.35f, 0.35f)

        val baseEyeVector = floatArrayOf(horizontalEyeOffset * 1.8f, -verticalEyeOffset * 1.6f, 1f)
        val worldGaze = PoseMath.rotateByEulerDegrees(
            vector = baseEyeVector,
            pitchXDeg = face.headEulerAngleX,
            yawYDeg = face.headEulerAngleY,
            rollZDeg = face.headEulerAngleZ
        )

        val vectorLengthPx = 280f
        val gazeEndPoint2D = PointF(
            eyesMiddle.x + worldGaze[0] * vectorLengthPx,
            eyesMiddle.y + worldGaze[1] * vectorLengthPx
        )

        // Heuristic: if eye contours remain stable under moderate roll, transparent glasses are tolerated.
        val glassesLikely = leftEyePoints.size >= 12 && rightEyePoints.size >= 12 && abs(face.headEulerAngleZ) < 35f

        return FacePoseResult(
            boundingBox = face.boundingBox,
            leftEyeCenter = leftCenter,
            rightEyeCenter = rightCenter,
            faceCenter = faceCenter,
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            eulerX = face.headEulerAngleX,
            eulerY = face.headEulerAngleY,
            eulerZ = face.headEulerAngleZ,
            gazeVector3D = worldGaze,
            gazeEndPoint2D = gazeEndPoint2D,
            glassesLikely = glassesLikely
        )
    }

    private fun averagePoint(points: List<PointF>): PointF {
        var sx = 0f
        var sy = 0f
        for (p in points) {
            sx += p.x
            sy += p.y
        }
        return PointF(sx / points.size, sy / points.size)
    }

    fun release() {
        detector.close()
    }
}
