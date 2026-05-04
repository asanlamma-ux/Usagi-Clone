package org.koharu.miyo.core.nativeio

import dagger.Reusable
import java.io.File
import javax.inject.Inject

@Reusable
class NativeZipWriter @Inject constructor() {

    val isAvailable: Boolean by lazy(LazyThreadSafetyMode.PUBLICATION) {
        try {
            System.loadLibrary("miyo-native")
            true
        } catch (e: UnsatisfiedLinkError) {
            false
        }
    }

    fun openZip(path: File, append: Boolean = false): Long {
        return nativeOpenZip(path.absolutePath, append)
    }

    fun closeZip(handle: Long) {
        nativeCloseZip(handle)
    }

    fun appendFileFromDisk(handle: Long, entryName: String, srcPath: File): Boolean {
        return nativeAppendFileFromDisk(handle, entryName, srcPath.absolutePath)
    }

    fun appendFileFromMemory(handle: Long, entryName: String, data: ByteArray, offset: Int = 0, length: Int = data.size): Boolean {
        return nativeAppendFileFromMemory(handle, entryName, data, offset, length)
    }

    fun benchmarkWrite(path: File, targetSizeMb: Int = 16): Long {
        return nativeBenchmarkWrite(path.absolutePath, targetSizeMb)
    }

    private external fun nativeOpenZip(path: String, append: Boolean): Long
    private external fun nativeCloseZip(handle: Long)
    private external fun nativeAppendFileFromDisk(handle: Long, entryName: String, srcPath: String): Boolean
    private external fun nativeAppendFileFromMemory(handle: Long, entryName: String, data: ByteArray, offset: Int, length: Int): Boolean
    private external fun nativeBenchmarkWrite(path: String, targetSizeMb: Int): Long
}
