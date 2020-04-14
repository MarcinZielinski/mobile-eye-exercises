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

    override fun onCameraFrame(inputFrame: CvCameraViewFrame): Mat { // TODO Auto-generated method stub
        mRgba = inputFrame.rgba()
        mGray = inputFrame.gray()

        val faces = MatOfRect()

        faceClassifier?.detectMultiScale(mGray, faces,1.3, 5)

        val facesArray = faces.toArray()

        for (face in facesArray) {
            Imgproc.rectangle(mRgba, Point(face.x.toDouble(), face.y.toDouble()),
                Point((face.x + face.width).toDouble(), (face.y + face.height).toDouble()), Scalar(255.0, 0.0, 0.0, 255.0), 3)
        }

        // Rotate mRgba 90 degrees
        Core.transpose(mRgba, mRgbaT)
        Imgproc.resize(mRgbaT, mRgbaF, mRgbaF!!.size(), 0.0, 0.0, 0)
        Core.flip(mRgbaF, mRgba, 1)

        return mRgba as Mat// This function must return
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
