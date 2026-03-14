package dev.mooner.dotenv.internal.platform

import dev.mooner.dotenv.DotenvException
import java.io.File

internal actual fun readEnvFile(path: String): String {
    val file = File(path)
    if (!file.exists()) throw DotenvException.FileNotFoundException(path)
    return file.readText(Charsets.UTF_8)
}

internal actual fun getSystemEnv(name: String): String? = System.getenv(name)

internal actual fun executeCommand(command: String): String {
    val isWindows = System.getProperty("os.name").lowercase().contains("win")
    val shell = if (isWindows) arrayOf("cmd", "/c", command) else arrayOf("sh", "-c", command)
    val process = ProcessBuilder(*shell)
        .redirectErrorStream(true)
        .start()
    val output = process.inputStream.bufferedReader().readText()
    process.waitFor()
    return output
}
