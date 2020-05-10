#include <jni.h>
#include <android/log.h>
#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>

#define TAG "NativeLib"

using namespace std;
using namespace cv;

extern "C"
JNIEXPORT void JNICALL
Java_pl_agh_eye_CameraActivity_adaptiveThresholdFromJNI(JNIEnv *env, jobject thiz, jlong mat_addr) {
    // get Mat from raw address
    Mat &mat = *(Mat *) mat_addr;

    clock_t begin = clock();

    cv::adaptiveThreshold(mat, mat, 255, ADAPTIVE_THRESH_MEAN_C, THRESH_BINARY_INV, 21, 5);

    // log computation time to Android Logcat
    double totalTime = double(clock() - begin) / CLOCKS_PER_SEC;
    __android_log_print(ANDROID_LOG_INFO, TAG, "adaptiveThreshold computation time = %f seconds\n",
                        totalTime);
}