package dev.mooner.dotenv.internal.platform

internal expect fun readEnvFile(path: String): String

internal expect fun getSystemEnv(name: String): String?

internal expect fun executeCommand(command: String): String
