@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package dev.mooner.dotenv.internal.platform

import dev.mooner.dotenv.DotenvException
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import platform.posix.fclose
import platform.posix.fgets
import platform.posix.fopen
import platform.posix.getenv

private const val CHUNK = 4096

internal actual fun readEnvFile(path: String): String {
    val file = fopen(path, "r") ?: throw DotenvException.FileNotFoundException(path)
    return try {
        buildString {
            memScoped {
                val buf = allocArray<ByteVar>(CHUNK + 1)
                while (fgets(buf, CHUNK, file) != null) {
                    append(buf.toKString())
                }
            }
        }
    } finally {
        fclose(file)
    }
}

internal actual fun getSystemEnv(name: String): String? = getenv(name)?.toKString()

// executeCommand is NOT provided here.
// - Unix-like targets (linuxX64, macosX64, macosArm64): see posixMain/ExecuteCommand.posix.kt
// - Windows (mingwX64): see mingwX64Main/ExecuteCommand.mingw.kt