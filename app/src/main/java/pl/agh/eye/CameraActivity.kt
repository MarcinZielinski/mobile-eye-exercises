package pl.agh.eye

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.SurfaceView
import android.view.WindowManager
import android.widget.SeekBar
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import kotlinx.android.synthetic.main.show_camera.*
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import org.opencv.objdetect.CascadeClassifier
import pl.agh.eye.exercise.Exercise
import pl.agh.eye.portrait_support.CameraBridgeViewBasePortraitSupport
import pl.agh.eye.portrait_support.CameraBridgeViewBasePortraitSupport.CvCameraViewFrame
import pl.agh.eye.portrait_support.CameraBridgeViewBasePortraitSupport.CvCameraViewListener2
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class CameraActivity : AppCompatActivity(), CvCameraViewListener2 {
    // Loads camera view of OpenCV for us to use. This lets us see using OpenCV
    private var mOpenCvCameraView: CameraBridgeViewBasePortraitSupport? = null

    // Used in Camera selection from menu (when implemented)
    private val mIsJavaCamera = true
    private val mItemSwitchCamera: MenuItem? = null

    // cascades
    private var faceClassifier: CascadeClassifier? = null
    private var eyeClassifier: CascadeClassifier? = null
    private var detectionUtils: DetectionUtils = DetectionUtils()

    // These variables are used (at the moment) to fix camera orientation from 270degree to 0degree
    var mRgba: Mat? = null
    var mGray: Mat? = null
    var mRgbaF: Mat? = null
    var mRgbaT: Mat? = null

    private var threshold = 60.0
    private val eyeRectThickness = 3

    private val mLoaderCallback: BaseLoaderCallback = object : BaseLoaderCallback(this) {
        override fun onManagerConnected(status: Int) {
            when (status) {
                LoaderCallbackInterface.SUCCESS -> {
                    Log.i(
                        TAG,
                        "OpenCV loaded successfully"
                    )
                    // Load native library after(!) OpenCV initialization
                    System.loadLibrary("native-lib")
                    mOpenCvCameraView!!.setCameraIndex(cameraIndex)
                    mOpenCvCameraView!!.enableView()

                    faceClassifier = getClassifier(
                        "haarcascade_frontalface_default.xml",
                        R.raw.haarcascade_frontalface_default
                    )
                    eyeClassifier = getClassifier(
                        "haarcascade_eye.xml",
                        R.raw.haarcascade_eye
                    )
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
        inputStream.copyTo(outputStream, 4096)
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

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(TAG, "called onCreate")
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        checkAndRequestPermissions()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.show_camera)
        mOpenCvCameraView = show_camera_activity_java_surface_view
        mOpenCvCameraView!!.visibility = SurfaceView.VISIBLE
        mOpenCvCameraView!!.setCvCameraViewListener(this)

        val exercise = intent.getSerializableExtra("exercise") as Exercise
        Log.i(TAG, "############################### " + exercise.title)

        switchCamerasButton.setOnClickListener {
            cameraIndex = if (cameraIndex == 1) 0 else 1
            mOpenCvCameraView!!.disableView()
            mOpenCvCameraView!!.setCameraIndex(cameraIndex)
            mOpenCvCameraView!!.enableView()
        }

        textViewTreshold.text = threshold.toBigDecimal().toPlainString()
        seekBarTreshold.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                threshold = progress.toDouble();
                textViewTreshold.text = threshold.toBigDecimal().toPlainString()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })

        seekBarTreshold.setProgress(threshold.toInt(), true);
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            CAMERA_PERMISSION_REQUEST -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mOpenCvCameraView!!.setCameraPermissionGranted()
                } else {
                    val message = "Camera permission was not granted"
                    Log.e(TAG, message)
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                }
            }
            else -> {
                Log.e(TAG, "Unexpected permission request")
            }
        }
    }

    public override fun onPause() {
        super.onPause()
        if (mOpenCvCameraView != null)
            mOpenCvCameraView!!.disableView()
    }

    override fun onResume() {
        super.onResume()
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization")
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback)
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!")
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
        }
    }

    public override fun onDestroy() {
        super.onDestroy()
        if (mOpenCvCameraView != null)
            mOpenCvCameraView!!.disableView()
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

    override fun onCameraFrame(inputFrame: CvCameraViewFrame): Mat {
        val currentOrientation = resources.configuration.orientation
        if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            mRgba = inputFrame.rgba()
            mGray = inputFrame.gray()
        } else {
            mRgba = inputFrame.rgba()
            mGray = inputFrame.gray()
            val rotImage = Imgproc.getRotationMatrix2D(
                Point(
                    (mRgba!!.cols() / 2).toDouble(),
                    (mRgba!!.rows() / 2).toDouble()
                ), if (cameraIndex == 1) 90.0 else -90.0, 1.0
            )
            Imgproc.warpAffine(mRgba, mRgba, rotImage, mRgba!!.size())
            Imgproc.warpAffine(mGray, mGray, rotImage, mRgba!!.size())
            if (cameraIndex == 1) {
                Core.flip(mRgba, mRgba, 1)
                Core.flip(mGray, mGray, 1)
            }
        }
        handleFaceAndEyesDetection()

        return mRgba as Mat
    }

    private fun handleFaceAndEyesDetection() {
        val facesArray = detectionUtils.detect(faceClassifier, mGray).toArray()
        if (facesArray!!.isNotEmpty()) {
            val face = facesArray[0]

            Imgproc.rectangle(
                mRgba,
                Point(face.x.toDouble(), face.y.toDouble()),
                Point((face.x + face.width).toDouble(), (face.y + face.height).toDouble()),
                Scalar(255.0, 0.0, 0.0, 255.0),
                eyeRectThickness
            )

            val grayFace = Mat(mGray, face)

            val eyesArray = detectionUtils.detect(eyeClassifier, grayFace).toArray()
            for (eye in eyesArray) {
                if (eye.y < face.height / 2) {
                    Imgproc.rectangle(
                        mRgba, Point((face.x + eye.x).toDouble(), (face.y + eye.y).toDouble()),
                        Point(
                            (face.x + eye.x + eye.width).toDouble(),
                            (face.y + eye.y + eye.height).toDouble()
                        ), Scalar(0.0, 255.0, 255.0, 255.0), eyeRectThickness
                    )

                    val eyeMat = Mat(grayFace, eye)
                    val eyeGaze = detectionUtils.getEyeGazeDirectionEdges(eyeMat, threshold)
                    if (eyeGaze != null)
                        Imgproc.circle(
                            mRgba, Point(
                                (face.x + eye.x + eyeGaze.x),
                                (face.y + eye.y + eyeMat.height() / 4 + eyeGaze.y)
                            ),
                            10, Scalar(255.0, 0.0, 255.0, 255.0), 3
                        )
                }
            }
        }
    }

    companion object {
        // Used for logging success or failure messages
        private const val TAG = "CameraActivity"
        private const val CAMERA_PERMISSION_REQUEST = 1
    }

    private fun checkAndRequestPermissions() {
        ActivityCompat.requestPermissions(
            this@CameraActivity,
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_REQUEST
        )
    }

    private external fun adaptiveThresholdFromJNI(matAddr: Long)
}
