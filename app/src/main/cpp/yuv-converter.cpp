#include <jni.h>
#include <cstdint>
#include <android/bitmap.h>
#include <android/log.h>

#if defined(__ARM_NEON) || defined(__ARM_NEON__)
#include <arm_neon.h>
#define MIYO_HAS_NEON 1
#else
#define MIYO_HAS_NEON 0
#endif

#define LOG_TAG "miyo-yuv"

/**
 * NEON-accelerated YUV NV21 to RGB conversion.
 * Falls back to scalar implementation if NEON is unavailable.
 */

static void yuvToRgbScalar(const uint8_t* yPlane, const uint8_t* uvPlane,
                           int width, int height, int stride,
                           uint8_t* rgbaDest, int rgbaStride) {
    for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
            int yIdx = y * stride + x;
            int uvIdx = (y / 2) * stride + (x & ~1);

            int Y = yPlane[yIdx] & 0xFF;
            int U = (uvPlane[uvIdx] & 0xFF) - 128;
            int V = (uvPlane[uvIdx + 1] & 0xFF) - 128;

            int R = Y + ((351 * V) >> 8);
            int G = Y - ((179 * V + 86 * U) >> 8);
            int B = Y + ((443 * U) >> 8);

            int destIdx = y * rgbaStride + x * 4;
            rgbaDest[destIdx]     = static_cast<uint8_t>(R < 0 ? 0 : (R > 255 ? 255 : R));
            rgbaDest[destIdx + 1] = static_cast<uint8_t>(G < 0 ? 0 : (G > 255 ? 255 : G));
            rgbaDest[destIdx + 2] = static_cast<uint8_t>(B < 0 ? 0 : (B > 255 ? 255 : B));
            rgbaDest[destIdx + 3] = 0xFF; // Alpha
        }
    }
}

#if MIYO_HAS_NEON
static void yuvToRgbNeon(const uint8_t* yPlane, const uint8_t* uvPlane,
                          int width, int height, int stride,
                          uint8_t* rgbaDest, int rgbaStride) {
    // Process 8 pixels at a time with NEON
    int16x8_t vCoeffR = vdupq_n_s16(351);   // V contribution to R
    int16x8_t vCoeffG_v = vdupq_n_s16(-179); // V contribution to G
    int16x8_t vCoeffG_u = vdupq_n_s16(-86);  // U contribution to G
    int16x8_t vCoeffB = vdupq_n_s16(443);    // U contribution to B
    int16x8_t vBias = vdupq_n_s16(-128);
    uint8x8_t vAlpha = vdup_n_u8(0xFF);

    int x = 0;
    for (; x <= width - 8; x += 8) {
        for (int y = 0; y < height; y++) {
            int yIdx = y * stride + x;
            int uvIdx = (y / 2) * stride + (x & ~1);

            uint8x8_t vY = vld1_u8(yPlane + yIdx);

            // Load U,V and interleave
            uint8x8_t vU = vld1_u8(uvPlane + uvIdx);
            uint8x8_t vV = vld1_u8(uvPlane + uvIdx + 1);

            int16x8_t sU = vaddw_s8(vBias, vreinterpret_s8_u8(vU));
            int16x8_t sV = vaddw_s8(vBias, vreinterpret_s8_u8(vV));

            int16x8_t sY = vreinterpretq_s16_u16(vmovl_u8(vY));

            // R = Y + 351*V/256
            // G = Y - 179*V/256 - 86*U/256
            // B = Y + 443*U/256
            int16x8_t sR = vaddq_s16(sY, vshrq_n_s16(vmulq_s16(vCoeffR, sV), 8));
            int16x8_t sG = vaddq_s16(sY, vshrq_n_s16(
                vaddq_s16(vmulq_s16(vCoeffG_v, sV), vmulq_s16(vCoeffG_u, sU)), 8));
            int16x8_t sB = vaddq_s16(sY, vshrq_n_s16(vmulq_s16(vCoeffB, sU), 8));

            uint8x8_t vR8 = vqmovun_s16(sR);
            uint8x8_t vG8 = vqmovun_s16(sG);
            uint8x8_t vB8 = vqmovun_s16(sB);

            // Interleave to RGBA
            uint8x8x4_t vRGBA;
            vRGBA.val[0] = vR8;
            vRGBA.val[1] = vG8;
            vRGBA.val[2] = vB8;
            vRGBA.val[3] = vAlpha;

            vst4_u8(rgbaDest + y * rgbaStride + x * 4, vRGBA);
        }
    }
}
#endif // MIYO_HAS_NEON

extern "C" {

JNIEXPORT void JNICALL
Java_org_koharu_miyo_core_image_NativeYuvConverter_nativeNv21ToRgba(
    JNIEnv* env, jclass clazz,
    jobject yBuffer, jobject uvBuffer,
    jint width, jint height, jint stride,
    jobject rgbaBitmap) {

#if MIYO_HAS_NEON
    // Use NEON path
    auto* yPlane = static_cast<const uint8_t*>(env->GetDirectBufferAddress(yBuffer));
    auto* uvPlane = static_cast<const uint8_t*>(env->GetDirectBufferAddress(uvBuffer));

    void* rgbaPixels = nullptr;
    if (AndroidBitmap_lockPixels(env, rgbaBitmap, &rgbaPixels) < 0) {
        return;
    }

    if (yPlane && uvPlane && rgbaPixels) {
        yuvToRgbNeon(yPlane, uvPlane, width, height, stride,
                     static_cast<uint8_t*>(rgbaPixels), width * 4);
    }

    AndroidBitmap_unlockPixels(env, rgbaBitmap);
#else
    // Scalar fallback
    auto* yPlane = static_cast<const uint8_t*>(env->GetDirectBufferAddress(yBuffer));
    auto* uvPlane = static_cast<const uint8_t*>(env->GetDirectBufferAddress(uvBuffer));

    void* rgbaPixels = nullptr;
    if (AndroidBitmap_lockPixels(env, rgbaBitmap, &rgbaPixels) < 0) {
        return;
    }

    if (yPlane && uvPlane && rgbaPixels) {
        yuvToRgbScalar(yPlane, uvPlane, width, height, stride,
                      static_cast<uint8_t*>(rgbaPixels), width * 4);
    }

    AndroidBitmap_unlockPixels(env, rgbaBitmap);
#endif
}

} // extern "C"
