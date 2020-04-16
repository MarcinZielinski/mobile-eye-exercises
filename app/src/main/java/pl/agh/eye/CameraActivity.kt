package pl.agh.eye

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.SurfaceView
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.show_camera.*
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.CameraBridgeViewBase
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import org.opencv.core.*
import org.opencv.features2d.FeatureDetector
import org.opencv.imgproc.Imgproc
import org.opencv.objdetect.CascadeClassifier
import pl.agh.eye.exercise.Exercise
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream


const val APPLICATION_SPECIFIC_PERMISSION_CODE = 7

class CameraActivity : AppCompatActivity(), CvCameraViewListener2 {
    // Loads camera view of OpenCV for us to use. This lets us see using OpenCV
    private var mOpenCvCameraView: CameraBridgeViewBase? = null

    // Used in Camera selection from menu (when implemented)
    private val mIsJavaCamera = true
    private val mItemSwitchCamera: MenuItem? = null

    // cascades
    private var faceClassifier: CascadeClassifier? = null
    private var eyeClassifier: CascadeClassifier? = null
    private var blobDetector: FeatureDetector? = null

    // These variables are used (at the moment) to fix camera orientation from 270degree to 0degree
    var mRgba: Mat? = null
    var mGray: Mat? = null
    var mRgbaF: Mat? = null
    var mRgbaT: Mat? = null

    private val mLoaderCallback: BaseLoaderCallback = object : BaseLoaderCallback(this) {
        override fun onManagerConnected(status: Int) {
            when (status) {
                LoaderCallbackInterface.SUCCESS -> {
                    Log.i(
                        TAG,
                        "OpenCV loaded successfully"
                    )
                    mOpenCvCameraView!!.enableView()

                    faceClassifier = getClassifier("haarcascade_frontalface_default.xml",
                        R.raw.haarcascade_frontalface_default)
                    eyeClassifier = getClassifier(
                        "haarcascade_eye.xml",
                        R.raw.haarcascade_eye
                    )
                    blobDetector = FeatureDetector.create(FeatureDetector.SIMPLEBLOB)
                    blobDetector!!.read("android.resource://pl.agh.eye/raw/blob.xml")
                    if (blobDetector!!.empty()) {
                        Log.i(
                            TAG,
                            "Blob fokt up"
                        )
                    }
                }
                else -> {
                    super.onManagerConnected(status)
                }
            }
        }
    }

    private fun getClassifier(name: String, cascadeResource: Int): CascadeClassifier {
        val inputStream: InputStream = resources.openRawResource(
            cascadeResource
        )
        val cascadeDir: File = getDir("cascade", Context.MODE_PRIVATE)
        val mCascadeFile = File(
            cascadeDir,
            name
        )
        val outputStream = FileOutputStream(mCascadeFile)

        val buffer = ByteArray(4096)
        var bytesRead: Int
        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            outputStream.write(buffer, 0, bytesRead)
        }
        inputStream.close()
        outputStream.close()
        val classifier = CascadeClassifier(mCascadeFile.absolutePath)

        if (classifier.empty()) {
            Log.e(TAG, "Failed to load cascade classifier")
        } else
            Log.i(
                TAG, "Loaded cascade classifier from "
                        + mCascadeFile.absolutePath
            )

        return classifier
    }

    init {
        Log.i(
            TAG,
            "Instantiated new " + this.javaClass
        )
    }

    // front = 1, back = 0
    private var cameraIndex = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(TAG, "called onCreate")
        super.onCreate(savedInstanceState)
        checkAndRequestPermissions()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.show_camera)
        mOpenCvCameraView = show_camera_activity_java_surface_view
        mOpenCvCameraView!!.visibility = SurfaceView.VISIBLE
        mOpenCvCameraView!!.setCvCameraViewListener(this)
        mOpenCvCameraView!!.enableView()

        val exercise = intent.getSerializableExtra("exercise") as Exercise
        Log.i(TAG, "############################### " + exercise.title)


        switchCamerasButton.setOnClickListener {
            cameraIndex = if (cameraIndex == 1) 0 else 1
            mOpenCvCameraView!!.disableView()
            mOpenCvCameraView!!.setCameraIndex(cameraIndex)
            mOpenCvCameraView!!.enableView()

        }
    }

    public override fun onPause() {
        super.onPause()
        mOpenCvCameraView?.disableView()
    }

    public override fun onResume() {
        super.onResume()
        if (!OpenCVLoader.initDebug()) {
            Log.d(
                TAG,
                "Internal OpenCV library not found. Using OpenCV Manager for initialization"
            )
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback)
        } else {
            Log.d(
                TAG,
                "OpenCV library found inside package. Using it!"
            )
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
        }
    }

    public override fun onDestroy() {
        super.onDestroy()
        mOpenCvCameraView?.disableView()
    }

    override fun onCameraViewStarted(width: Int, height: Int) {
        mRgba = Mat(height, width, CvType.CV_8UC4)
        mGray = Mat(height, width, CvType.CV_8UC4)
        mRgbaF = Mat(height, width, CvType.CV_8UC4)
        mRgbaT = Mat(width, width, CvType.CV_8UC4)
        Log.i(TAG, "camera view started")
    }

    override fun onCameraViewStopped() {
        mRgba!!.release()
        mGray!!.release()
        Log.i(TAG, "camera view stopped")
    }

    private fun detect(classifier: CascadeClassifier?, img: Mat?): MatOfRect {
        val elems = MatOfRect()
        classifier?.detectMultiScale(img, elems,1.3, 5)
        return elems
    }

    override fun onCameraFrame(inputFrame: CvCameraViewFrame): Mat {
        mRgba = inputFrame.rgba()
        mGray = inputFrame.gray()

        val facesArray = detect(faceClassifier, mGray).toArray()
        if (facesArray!!.isNotEmpty()) {
            val face = facesArray[0]

            Imgproc.rectangle(mRgba, Point(face.x.toDouble(), face.y.toDouble()),
                Point((face.x + face.width).toDouble(), (face.y + face.height).toDouble()), Scalar(255.0, 0.0, 0.0, 255.0), 3)

            val grayFace = Mat(mGray, face)

            val eyesArray = detect(eyeClassifier, grayFace).toArray()
            for (eye in eyesArray) {
                if (eye.y < face.height / 2) {
                    Imgproc.rectangle(mRgba, Point((face.x + eye.x).toDouble(), (face.y + eye.y).toDouble()),
                        Point((face.x + eye.x + eye.width).toDouble(), (face.y + eye.y + eye.height).toDouble()), Scalar(0.0, 255.0, 255.0, 255.0), 3)

                    getEyeGazeDirection(Mat(grayFace, eye), face, eye)
                }
            }
        }

        // Rotate mRgba 90 degrees
        Core.transpose(mRgba, mRgbaT)
        Imgproc.resize(mRgbaT, mRgbaF, mRgbaF!!.size(), 0.0, 0.0, 0)
        Core.flip(mRgbaF, mRgba, 1)

        return mRgba as Mat// This function must return
    }

    private fun getEyeGazeDirection(eyeMat: Mat?, face: Rect, eye: Rect) {
        val browlessEye = Mat(eyeMat, Rect(0, eyeMat!!.height() / 4, eyeMat.width(), eyeMat.height() - eyeMat.height() / 4))
        val blobEye = MatOfRect()
        Imgproc.threshold(browlessEye, blobEye, 80.0, 255.0, Imgproc.THRESH_BINARY)

        Imgproc.erode(blobEye, blobEye, Mat(), Point(-1.0, -1.0), 2)
        Imgproc.dilate(blobEye, blobEye, Mat(), Point(-1.0, -1.0), 4)
        Imgproc.medianBlur(blobEye, blobEye, 5)

        drawIrisByContour(blobEye, face, eye, eyeMat)

        // Imgproc.resize(blobEye, mRgba, mRgba!!.size(), 0.0, 0.0)
    }

    private fun drawIrisByMeanCoords(blobEye: Mat?, face: Rect, eye:Rect, eyeMat: Mat) {
        var centerX = 0
        var centerY = 0
        var pointsCount = 0
        for (x in 0 until blobEye!!.cols()) {
            for (y in 0 until blobEye.rows()) {
                if (blobEye.get(y, x).isNotEmpty()) {
                    if (blobEye.get(y, x)[0] != 255.0){
                        centerX += x
                        centerY += y
                        pointsCount++
                    }
                }
            }
        }
        if (pointsCount != 0) {
            Imgproc.circle(mRgba, Point((face.x + eye.x + centerX/pointsCount).toDouble(),
                (face.y + eye.y + eyeMat.height() / 4 + centerY/pointsCount).toDouble()),
                10, Scalar(255.0, 0.0, 255.0, 255.0), 3)
        }
    }

    private fun drawIrisByContour(blobEye: Mat?, face: Rect, eye:Rect, eyeMat: Mat) {
        val contours = ArrayList<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(blobEye, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE)

        for (cnt in contours) {
            val boundingBox = Imgproc.boundingRect(cnt)
            if (boundingBox.width < eye.width / 1.5) {
                Imgproc.rectangle(mRgba, Point((face.x + eye.x + boundingBox.x).toDouble(), (face.y + eye.y + boundingBox.y + eyeMat.height() / 4).toDouble()),
                    Point((face.x + eye.x + boundingBox.x + boundingBox.width).toDouble(), (face.y + eye.y + boundingBox.y + boundingBox.height + eyeMat.height() / 4).toDouble()),
                    Scalar(255.0, 0.0, 255.0, 255.0), 3)
            }
        }
    }

    companion object {
        // Used for logging success or failure messages
        private const val TAG = "OCVSample::Activity"
    }

    private fun checkAndRequestPermissions(): Boolean {
        val camera = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        )
        val listPermissionsNeeded: MutableList<String> = ArrayList()
        if (camera != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.CAMERA)
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                listPermissionsNeeded.toTypedArray(),
                APPLICATION_SPECIFIC_PERMISSION_CODE
            )
            return false
        }
        return true
    }
}
