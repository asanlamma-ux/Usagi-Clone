package org.koharu.miyo.core.nativeio

import dagger.Reusable
import java.io.File
import javax.inject.Inject

@Reusable
class NativeImageProbe @Inject constructor() {

    val isAvailable: Boolean by lazy(LazyThreadSafetyMode.PUBLICATION) {
        try {
            System.loadLibrary("miyo-native")
            true
        } catch (e: UnsatisfiedLinkError) {
            false
        }
    }

    fun probeFormat(file: File): String {
        return nativeProbeFormat(file.absolutePath) ?: ""
    }

    private external fun nativeProbeFormat(filePath: String): String?
}
