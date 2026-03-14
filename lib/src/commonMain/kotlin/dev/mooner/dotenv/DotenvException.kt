package dev.mooner.dotenv

/**
 * Base class for all exceptions thrown by the Dotenv library.
 */
sealed class DotenvException(message: String, cause: Throwable? = null) : Exception(message, cause) {

    /**
     * Thrown when the specified .env file does not exist or cannot be opened.
     */
    class FileNotFoundException(val path: String) : DotenvException(
        "Dotenv file not found: '$path'"
    )

    /**
     * Thrown when the .env file contains a syntax error.
     *
     * @param detail Human-readable description of the problem.
     * @param line   1-based line number where the error was detected, if available.
     */
    class ParseException(detail: String, val line: Int? = null) : DotenvException(
        if (line != null) "Parse error at line $line: $detail" else "Parse error: $detail"
    )

    /**
     * Thrown by [Dotenv.getOrThrow] when the requested key is absent.
     */
    class MissingKeyException(val key: String) : DotenvException(
        "Required environment variable '$key' is not defined"
    )

    /**
     * Thrown when a command substitution `$(command)` fails during parsing.
     */
    class CommandSubstitutionException(val command: String, cause: Throwable? = null) : DotenvException(
        "Command substitution failed for: '$command'", cause
    )
}
