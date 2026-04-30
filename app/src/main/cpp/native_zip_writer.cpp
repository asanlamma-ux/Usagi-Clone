#include <jni.h>
#include <android/log.h>
#include <zlib.h>
#include <cstring>
#include <cstdio>
#include <string>
#include <chrono>

#define LOG_TAG "UsagiNative"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern "C" {

JNIEXPORT jlong JNICALL
Java_org_draken_usagi_core_native_NativeZipWriter_nativeOpenZip(
    JNIEnv* env, jclass clazz, jstring path, jboolean append) {
    const char* pathStr = env->GetStringUTFChars(path, nullptr);
    const char* mode = append ? "ab" : "wb";
    FILE* file = fopen(pathStr, mode);
    env->ReleaseStringUTFChars(path, pathStr);
    if (!file) {
        LOGE("Failed to open ZIP file");
        return 0;
    }
    return reinterpret_cast<jlong>(file);
}

JNIEXPORT void JNICALL
Java_org_draken_usagi_core_native_NativeZipWriter_nativeCloseZip(
    JNIEnv* env, jclass clazz, jlong handle) {
    FILE* file = reinterpret_cast<FILE*>(handle);
    if (file) {
        fclose(file);
    }
}

JNIEXPORT jboolean JNICALL
Java_org_draken_usagi_core_native_NativeZipWriter_nativeAppendFileFromDisk(
    JNIEnv* env, jclass clazz, jlong handle, jstring entryName, jstring srcPath) {
    FILE* dest = reinterpret_cast<FILE*>(handle);
    if (!dest) return JNI_FALSE;
    
    const char* entryStr = env->GetStringUTFChars(entryName, nullptr);
    const char* srcStr = env->GetStringUTFChars(srcPath, nullptr);
    
    FILE* src = fopen(srcStr, "rb");
    if (!src) {
        env->ReleaseStringUTFChars(entryName, entryStr);
        env->ReleaseStringUTFChars(srcPath, srcStr);
        return JNI_FALSE;
    }
    
    fseek(src, 0, SEEK_END);
    long srcSize = ftell(src);
    fseek(src, 0, SEEK_SET);
    
    unsigned long crc = crc32(0L, Z_NULL, 0);
    unsigned char buffer[65536];
    size_t bytesRead;
    long totalWritten = 0;
    long dataStartPos = ftell(dest);
    
    fwrite("PK\003\004", 1, 4, dest);
    // Version needed, flags, compression method (0=store)
    unsigned char header[] = {20, 0, 0, 0, 0, 0, 0, 0};
    fwrite(header, 1, 8, dest);
    
    // CRC32 placeholder
    unsigned long crcPlaceholder = 0;
    fwrite(&crcPlaceholder, 1, 4, dest);
    
    // Compressed/uncompressed size placeholders
    unsigned long sizePlaceholder = (unsigned long)srcSize;
    fwrite(&sizePlaceholder, 1, 4, dest);
    fwrite(&sizePlaceholder, 1, 4, dest);
    
    // File name length, extra field length
    unsigned short nameLen = strlen(entryStr);
    unsigned short extraLen = 0;
    fwrite(&nameLen, 1, 2, dest);
    fwrite(&extraLen, 1, 2, dest);
    
    // File name
    fwrite(entryStr, 1, nameLen, dest);
    
    // File data
    while ((bytesRead = fread(buffer, 1, sizeof(buffer), src)) > 0) {
        crc = crc32(crc, buffer, bytesRead);
        fwrite(buffer, 1, bytesRead, dest);
        totalWritten += bytesRead;
    }
    fclose(src);
    
    // Write back CRC and sizes
    long endPos = ftell(dest);
    fseek(dest, dataStartPos + 14, SEEK_SET);
    fwrite(&crc, 1, 4, dest);
    fwrite(&sizePlaceholder, 1, 4, dest);
    fwrite(&sizePlaceholder, 1, 4, dest);
    fseek(dest, endPos, SEEK_SET);
    
    env->ReleaseStringUTFChars(entryName, entryStr);
    env->ReleaseStringUTFChars(srcPath, srcStr);
    
    LOGD("Appended %s: %ld bytes, CRC=0x%lx", entryStr, totalWritten, crc);
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL
Java_org_draken_usagi_core_native_NativeZipWriter_nativeAppendFileFromMemory(
    JNIEnv* env, jclass clazz, jlong handle, jstring entryName,
    jbyteArray data, jint offset, jint length) {
    FILE* dest = reinterpret_cast<FILE*>(handle);
    if (!dest) return JNI_FALSE;
    
    const char* entryStr = env->GetStringUTFChars(entryName, nullptr);
    jbyte* dataPtr = env->GetByteArrayElements(data, nullptr);
    
    unsigned long crc = crc32(0L, Z_NULL, 0);
    crc = crc32(crc, reinterpret_cast<const unsigned char*>(dataPtr + offset), length);
    
    long dataStartPos = ftell(dest);
    fwrite("PK\003\004", 1, 4, dest);
    unsigned char header[] = {20, 0, 0, 0, 0, 0, 0, 0};
    fwrite(header, 1, 8, dest);
    
    unsigned long crcVal = crc;
    fwrite(&crcVal, 1, 4, dest);
    
    unsigned long sizeVal = (unsigned long)length;
    fwrite(&sizeVal, 1, 4, dest);
    fwrite(&sizeVal, 1, 4, dest);
    
    unsigned short nameLen = strlen(entryStr);
    unsigned short extraLen = 0;
    fwrite(&nameLen, 1, 2, dest);
    fwrite(&extraLen, 1, 2, dest);
    fwrite(entryStr, 1, nameLen, dest);
    fwrite(dataPtr + offset, 1, length, dest);
    
    env->ReleaseByteArrayElements(data, dataPtr, JNI_ABORT);
    env->ReleaseStringUTFChars(entryName, entryStr);
    
    return JNI_TRUE;
}

JNIEXPORT jlong JNICALL
Java_org_draken_usagi_core_native_NativeZipWriter_nativeBenchmarkWrite(
    JNIEnv* env, jclass clazz, jstring path, jint targetSizeMb) {
    const char* pathStr = env->GetStringUTFChars(path, nullptr);
    FILE* file = fopen(pathStr, "wb");
    env->ReleaseStringUTFChars(path, pathStr);
    
    if (!file) {
        LOGE("Failed to open benchmark file");
        return -1;
    }
    
    unsigned char buffer[65536];
    memset(buffer, 0xFF, sizeof(buffer));
    
    long totalBytes = (long)targetSizeMb * 1024 * 1024;
    long bytesWritten = 0;
    
    auto start = std::chrono::high_resolution_clock::now();
    
    while (bytesWritten < totalBytes) {
        size_t toWrite = sizeof(buffer);
        if (bytesWritten + toWrite > totalBytes) {
            toWrite = totalBytes - bytesWritten;
        }
        fwrite(buffer, 1, toWrite, file);
        bytesWritten += toWrite;
    }
    
    auto end = std::chrono::high_resolution_clock::now();
    auto elapsedMs = std::chrono::duration_cast<std::chrono::milliseconds>(end - start).count();
    
    fclose(file);
    remove(pathStr);
    
    double mbps = (targetSizeMb * 1000.0) / elapsedMs;
    LOGD("Benchmark: %dMB in %lldms (%.1f MB/s)", targetSizeMb, (long long)elapsedMs, mbps);
    
    return static_cast<jlong>(mbps * 1000);
}

} // extern "C"
