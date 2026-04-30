package org.draken.usagi.core.nativeio

import dagger.Reusable
import java.io.File
import javax.inject.Inject

@Reusable
class NativeImageProbe @Inject constructor() {

    val isAvailable: Boolean
        get() = try {
            System.loadLibrary("usagi-native")
            true
        } catch (e: UnsatisfiedLinkError) {
            false
        }

    fun probeFormat(file: File): String {
        return nativeProbeFormat(file.absolutePath) ?: ""
    }

    private external fun nativeProbeFormat(filePath: String): String?
}