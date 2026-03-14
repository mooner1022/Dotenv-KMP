package dev.mooner.dotenv.internal.platform

import dev.mooner.dotenv.DotenvException

// popen/pclose are C preprocessor macros on Windows (MinGW defines them as
// aliases for _popen/_pclose). Kotlin/Native's C interop does not expose macros,
// so they are unavailable on the mingwX64 target.
//
// Command substitution via $(command) is therefore unsupported on this target.
// Users on Windows should use the JVM target, which implements executeCommand
// through ProcessBuilder with full shell support.
internal actual fun executeCommand(command: String): String {
    throw DotenvException.CommandSubstitutionException(
        command,
        UnsupportedOperationException(
            "Command substitution is not supported on the mingwX64 native target. " +
            "popen/pclose are C macros on Windows and are not exposed by Kotlin/Native interop. " +
            "Use the JVM target on Windows for full command substitution support."
        )
    )
}
