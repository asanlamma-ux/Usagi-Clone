#include <jni.h>
#include <android/log.h>

#define LOG_TAG "miyo-jpeg-stub"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

/**
 * Stub JNI methods for when libjpeg-turbo is not compiled in.
 * These return error codes so the Kotlin layer falls back to Android BitmapFactory.
 */

extern "C" {

JNIEXPORT jint JNICALL
Java_org_koharu_miyo_core_image_NativeJpegDecoder_nativeDecodeJpeg(
    JNIEnv* env, jclass clazz,
    jbyteArray data, jobject bitmap,
    jintArray outWidth, jintArray outHeight) {
    LOGI("JPEG decoder not compiled - using fallback");
    return -99; // Signal fallback needed
}

JNIEXPORT jint JNICALL
Java_org_koharu_miyo_core_image_NativeJpegDecoder_nativeProbeJpeg(
    JNIEnv* env, jclass clazz,
    jbyteArray data,
    jintArray outWidth, jintArray outHeight) {
    LOGI("JPEG prober not compiled - using fallback");
    return -99; // Signal fallback needed
}

} // extern "C"