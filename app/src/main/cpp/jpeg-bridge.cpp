#include "jpeg-bridge.h"
#include <cstdio>
#include <cstring>
#include <csetjmp>
#include <android/log.h>

extern "C" {
#include "jpeglib.h"
}

#define LOG_TAG "miyo-jpeg"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

struct JpegErrorMgr {
    struct jpeg_error_mgr pub;
    jmp_buf setjmpBuffer;
};

static void jpegErrorExit(j_common_ptr cinfo) {
    auto* myErr = reinterpret_cast<JpegErrorMgr*>(cinfo->err);
    (*cinfo->err->output_message)(cinfo);
    longjmp(myErr->setjmpBuffer, 1);
}

int decodeJpegToBitmap(JNIEnv* env, const uint8_t* jpegData, size_t jpegSize,
                       jobject bitmap, int* outWidth, int* outHeight) {
    if (!jpegData || jpegSize == 0 || !bitmap) {
        return -1;
    }

    AndroidBitmapInfo bitmapInfo;
    if (AndroidBitmap_getInfo(env, bitmap, &bitmapInfo) < 0) {
        LOGE("Failed to get bitmap info");
        return -2;
    }

    // Lock the bitmap pixels for writing
    void* bitmapPixels = nullptr;
    if (AndroidBitmap_lockPixels(env, bitmap, &bitmapPixels) < 0) {
        LOGE("Failed to lock bitmap pixels");
        return -3;
    }

    // Setup JPEG decompressor
    struct jpeg_decompress_struct cinfo;
    struct JpegErrorMgr jerr;

    cinfo.err = jpeg_std_error(&jerr.pub);
    jerr.pub.error_exit = jpegErrorExit;

    if (setjmp(jerr.setjmpBuffer)) {
        jpeg_destroy_decompress(&cinfo);
        AndroidBitmap_unlockPixels(env, bitmap);
        LOGE("JPEG error during decompression");
        return -4;
    }

    jpeg_create_decompress(&cinfo);
    jpeg_mem_src(&cinfo, jpegData, jpegSize);

    if (jpeg_read_header(&cinfo, TRUE) != JPEG_HEADER_OK) {
        jpeg_destroy_decompress(&cinfo);
        AndroidBitmap_unlockPixels(env, bitmap);
        LOGE("Invalid JPEG header");
        return -5;
    }

    *outWidth = cinfo.image_width;
    *outHeight = cinfo.image_height;

    // Ensure dimensions match the bitmap
    if (static_cast<uint32_t>(*outWidth) != bitmapInfo.width ||
        static_cast<uint32_t>(*outHeight) != bitmapInfo.height) {
        jpeg_destroy_decompress(&cinfo);
        AndroidBitmap_unlockPixels(env, bitmap);
        LOGE("Dimension mismatch: JPEG=%dx%d, Bitmap=%dx%d",
             *outWidth, *outHeight, bitmapInfo.width, bitmapInfo.height);
        return -6;
    }

    // Choose fast decompression options
    cinfo.dct_method = JDCT_IFAST;
    cinfo.do_fancy_upsampling = FALSE;
    cinfo.two_pass_quantize = FALSE;
    cinfo.dither_mode = JDITHER_ORDERED;

    jpeg_start_decompress(&cinfo);

    // Decode row by row directly into bitmap
    auto* rowBuffer = new JSAMPROW[1];
    auto* destRow = static_cast<uint8_t*>(bitmapPixels);

    while (cinfo.output_scanline < cinfo.output_height) {
        rowBuffer[0] = destRow + cinfo.output_scanline * bitmapInfo.stride;
        jpeg_read_scanlines(&cinfo, rowBuffer, 1);
    }

    delete[] rowBuffer;

    jpeg_finish_decompress(&cinfo);
    jpeg_destroy_decompress(&cinfo);
    AndroidBitmap_unlockPixels(env, bitmap);

    return 0;
}

int probeJpegDimensions(const uint8_t* jpegData, size_t jpegSize,
                        int* outWidth, int* outHeight) {
    if (!jpegData || jpegSize == 0) {
        return -1;
    }

    struct jpeg_decompress_struct cinfo;
    struct JpegErrorMgr jerr;

    cinfo.err = jpeg_std_error(&jerr.pub);
    jerr.pub.error_exit = jpegErrorExit;

    if (setjmp(jerr.setjmpBuffer)) {
        jpeg_destroy_decompress(&cinfo);
        return -2;
    }

    jpeg_create_decompress(&cinfo);
    jpeg_mem_src(&cinfo, jpegData, jpegSize);

    if (jpeg_read_header(&cinfo, TRUE) != JPEG_HEADER_OK) {
        jpeg_destroy_decompress(&cinfo);
        return -3;
    }

    *outWidth = cinfo.image_width;
    *outHeight = cinfo.image_height;

    jpeg_destroy_decompress(&cinfo);
    return 0;
}

JNIEXPORT jint JNICALL
Java_org_koharu_miyo_core_image_NativeJpegDecoder_nativeDecodeJpeg(
    JNIEnv* env, jclass clazz,
    jbyteArray data, jobject bitmap,
    jintArray outWidth, jintArray outHeight) {
    if (!data || !bitmap || !outWidth || !outHeight) {
        return -1;
    }

    jbyte* dataPtr = env->GetByteArrayElements(data, nullptr);
    if (!dataPtr) {
        return -2;
    }

    int width = 0;
    int height = 0;
    int result = decodeJpegToBitmap(
        env,
        reinterpret_cast<const uint8_t*>(dataPtr),
        static_cast<size_t>(env->GetArrayLength(data)),
        bitmap,
        &width,
        &height
    );

    env->ReleaseByteArrayElements(data, dataPtr, JNI_ABORT);

    if (result == 0) {
        env->SetIntArrayRegion(outWidth, 0, 1, &width);
        env->SetIntArrayRegion(outHeight, 0, 1, &height);
    }
    return result;
}

JNIEXPORT jint JNICALL
Java_org_koharu_miyo_core_image_NativeJpegDecoder_nativeProbeJpeg(
    JNIEnv* env, jclass clazz,
    jbyteArray data,
    jintArray outWidth, jintArray outHeight) {
    if (!data || !outWidth || !outHeight) {
        return -1;
    }

    jbyte* dataPtr = env->GetByteArrayElements(data, nullptr);
    if (!dataPtr) {
        return -2;
    }

    int width = 0;
    int height = 0;
    int result = probeJpegDimensions(
        reinterpret_cast<const uint8_t*>(dataPtr),
        static_cast<size_t>(env->GetArrayLength(data)),
        &width,
        &height
    );

    env->ReleaseByteArrayElements(data, dataPtr, JNI_ABORT);

    if (result == 0) {
        env->SetIntArrayRegion(outWidth, 0, 1, &width);
        env->SetIntArrayRegion(outHeight, 0, 1, &height);
    }
    return result;
}
