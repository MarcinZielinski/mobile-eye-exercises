package pl.agh.eye

import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import org.opencv.objdetect.CascadeClassifier

class DetectionUtils {
    fun detect(classifier: CascadeClassifier?, img: Mat?): MatOfRect {
        val elems = MatOfRect()
        classifier?.detectMultiScale(img, elems, 1.3, 5)
        return elems
    }

    fun getEyeGazeDirectionEdges(eyeMat: Mat?, threshold: Double): Point {
        val browlessEye = Mat(
            eyeMat,
            Rect(0, eyeMat!!.height() / 4, eyeMat.width(), eyeMat.height() / 2)
        )

        val blobEye = MatOfRect()
        Imgproc.threshold(browlessEye, blobEye, threshold, 255.0, Imgproc.THRESH_BINARY)
        Imgproc.Canny(blobEye, blobEye, 5.0, 70.0, 3)
        Imgproc.GaussianBlur(blobEye, blobEye, Size(3.0, 3.0), 0.0, 0.0)

        return getIrisPosByMeanCoords(blobEye, 0.0)
    }

    fun getEyeGazeDirectionThreshold(eyeMat: Mat?, threshold: Double): Point {
        val browlessEye = Mat(
            eyeMat,
            Rect(0, eyeMat!!.height() / 4, eyeMat.width(), eyeMat.height() - eyeMat.height() / 4)
        )
        val blobEye = MatOfRect()
        Imgproc.threshold(browlessEye, blobEye, threshold, 255.0, Imgproc.THRESH_BINARY)

        Imgproc.erode(blobEye, blobEye, Mat(), Point(-1.0, -1.0), 2)
        Imgproc.dilate(blobEye, blobEye, Mat(), Point(-1.0, -1.0), 4)
        Imgproc.medianBlur(blobEye, blobEye, 5)

        return getIrisPosByMeanCoords(blobEye, 255.0)
    }

    private fun getIrisPosByMeanCoords(blobEye: Mat?, cutColor: Double): Point {
        var centerX = 0
        var centerY = 0
        var pointsCount = 0
        for (x in 0 until blobEye!!.cols()) {
            for (y in 0 until blobEye.rows()) {
                if (blobEye.get(y, x).isNotEmpty()) {
                    if (blobEye.get(y, x)[0] != cutColor) {
                        centerX += x
                        centerY += y
                        pointsCount++
                    }
                }
            }
        }
        return if (pointsCount == 0) Point(-1.0, -1.0) else
            Point((centerX / pointsCount).toDouble(), (centerY / pointsCount).toDouble())
    }

    private fun getIrisPosByContour(blobEye: Mat?): Point {
        val contours = ArrayList<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(
            blobEye,
            contours,
            hierarchy,
            Imgproc.RETR_TREE,
            Imgproc.CHAIN_APPROX_SIMPLE
        )

        var numOfBoxes = 0
        var bestBoundingBox = Rect()
        for (cnt in contours) {
            val boundingBox = Imgproc.boundingRect(cnt)
            if (boundingBox.width < blobEye!!.width() / 1.5) {
                if (boundingBox.width * boundingBox.height > bestBoundingBox.width * bestBoundingBox.height)
                    bestBoundingBox = boundingBox
                numOfBoxes += 1
            }
        }

        return if (numOfBoxes == 0) Point(-1.0, -1.0) else
            Point(
                (bestBoundingBox.x + bestBoundingBox.width / 2).toDouble(),
                (bestBoundingBox.y + bestBoundingBox.height / 2).toDouble()
            )
    }
}