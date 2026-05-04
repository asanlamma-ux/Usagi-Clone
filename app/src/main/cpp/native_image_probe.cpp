#include <jni.h>
#include <android/log.h>
#include <cstdio>
#include <cstring>

#define LOG_TAG "MiyoImage"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

extern "C" {

JNIEXPORT jstring JNICALL
Java_org_koharu_miyo_core_nativeio_NativeImageProbe_nativeProbeFormat(
    JNIEnv* env, jobject thiz, jstring filePath) {
    const char* path = env->GetStringUTFChars(filePath, nullptr);
    if (!path) return nullptr;

    FILE* file = fopen(path, "rb");
    env->ReleaseStringUTFChars(filePath, path);

    if (!file) {
        return env->NewStringUTF("");
    }

    unsigned char header[16];
    size_t bytesRead = fread(header, 1, sizeof(header), file);
    fclose(file);

    // JPEG: FF D8 FF
    if (bytesRead >= 3 && header[0] == 0xFF && header[1] == 0xD8 && header[2] == 0xFF) {
        return env->NewStringUTF("image/jpeg");
    }
    // PNG: 89 50 4E 47
    if (bytesRead >= 4 && header[0] == 0x89 && header[1] == 0x50 &&
        header[2] == 0x4E && header[3] == 0x47) {
        return env->NewStringUTF("image/png");
    }
    // GIF: 47 49 46
    if (bytesRead >= 3 && header[0] == 'G' && header[1] == 'I' && header[2] == 'F') {
        return env->NewStringUTF("image/gif");
    }
    // WebP: RIFF....WEBP
    if (bytesRead >= 12 && header[0] == 'R' && header[1] == 'I' &&
        header[2] == 'F' && header[3] == 'F' &&
        header[8] == 'W' && header[9] == 'E' &&
        header[10] == 'B' && header[11] == 'P') {
        return env->NewStringUTF("image/webp");
    }
    // BMP: 42 4D
    if (bytesRead >= 2 && header[0] == 'B' && header[1] == 'M') {
        return env->NewStringUTF("image/bmp");
    }
    // AVIF: ftypavif
    if (bytesRead >= 12 && header[4] == 'f' && header[5] == 't' &&
        header[6] == 'y' && header[7] == 'p' &&
        header[8] == 'a' && header[9] == 'v' &&
        header[10] == 'i' && header[11] == 'f') {
        return env->NewStringUTF("image/avif");
    }
    // HEIF/HEIC: ftyp box
    if (bytesRead >= 12 && header[4] == 'f' && header[5] == 't' &&
        header[6] == 'y' && header[7] == 'p') {
        return env->NewStringUTF("image/heif");
    }

    return env->NewStringUTF("");
}

} // extern "C"
