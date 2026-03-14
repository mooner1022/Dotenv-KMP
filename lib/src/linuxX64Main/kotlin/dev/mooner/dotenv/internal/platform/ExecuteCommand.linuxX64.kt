@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package dev.mooner.dotenv.internal.platform

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import platform.posix.fgets
import platform.posix.pclose
import platform.posix.popen

private const val CHUNK = 4096

internal actual fun executeCommand(command: String): String {
    val pipe = popen(command, "r")
        ?: throw RuntimeException("Failed to open pipe for command: '$command'")
    return try {
        buildString {
            memScoped {
                val buf = allocArray<ByteVar>(CHUNK + 1)
                while (fgets(buf, CHUNK, pipe) != null) {
                    append(buf.toKString())
                }
            }
        }
    } finally {
        pclose(pipe)
    }
}
