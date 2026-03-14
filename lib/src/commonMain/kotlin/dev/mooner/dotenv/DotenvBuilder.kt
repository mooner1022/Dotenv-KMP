package dev.mooner.dotenv

import dev.mooner.dotenv.internal.parser.DotenvParser
import dev.mooner.dotenv.internal.platform.executeCommand
import dev.mooner.dotenv.internal.platform.getSystemEnv
import dev.mooner.dotenv.internal.platform.readEnvFile

@DslMarker
annotation class DotenvDsl

/**
 * Builder for configuring a [Dotenv] instance.
 * Use via the [dotenv] DSL function.
 */
@DotenvDsl
class DotenvBuilder internal constructor() {

    /**
     * Path to the .env file. Defaults to `".env"` (current working directory).
     */
    var path: String = ".env"

    /**
     * Parsing dialect. Defaults to [DotenvFormat.Original].
     */
    var format: DotenvFormat = DotenvFormat.Original

    /**
     * When `true`, values from the .env file take precedence over identically
     * named system environment variables during variable interpolation lookups.
     *
     * When `false` (default), a variable already present in the system
     * environment is preferred over the .env file definition.
     */
    var overrideSystemEnv: Boolean = false

    /**
     * When `true`, `$(command)` substitutions in Dotenvy format are executed.
     * Has no effect with [DotenvFormat.Original].
     * Defaults to `false` for safety.
     */
    var enableCommandSubstitution: Boolean = false

    /**
     * When `true`, a missing .env file is silently ignored and an empty [Dotenv]
     * is returned instead of throwing [DotenvException.FileNotFoundException].
     */
    var ignoreIfMissing: Boolean = false

    /**
     * When set, this string is parsed directly as .env content, bypassing
     * all file I/O. [path] and [ignoreIfMissing] are ignored when this is set.
     *
     * Useful for parsing env content from sources other than the filesystem
     * (memory, network, encrypted storage, tests, etc.).
     */
    var content: String? = null
}

/**
 * Loads and parses a .env file, returning a [Dotenv] instance.
 *
 * ```kotlin
 * val env = dotenv {
 *     path   = "/etc/myapp/.env"
 *     format = DotenvFormat.Dotenvy
 * }
 * ```
 *
 * @param block Configuration block applied to a [DotenvBuilder].
 * @throws DotenvException.FileNotFoundException if the file is missing and
 *   [DotenvBuilder.ignoreIfMissing] is `false`.
 * @throws DotenvException.ParseException on malformed input (reserved for future use).
 */
fun dotenv(block: DotenvBuilder.() -> Unit = {}): Dotenv {
    val cfg = DotenvBuilder().apply(block)

    val content: String = when (val inlined = cfg.content) {
        null -> try {
            readEnvFile(cfg.path)
        } catch (e: DotenvException.FileNotFoundException) {
            if (cfg.ignoreIfMissing) return Dotenv(emptyMap())
            throw e
        }
        else -> inlined
    }

    // Build the system-env resolver, respecting override preference.
    val systemLookup: (String) -> String? = if (cfg.overrideSystemEnv) {
        // .env wins → during interpolation, look in already-parsed vars first;
        // system env is a fallback only for truly unresolved names.
        { name -> getSystemEnv(name) }
    } else {
        // System env wins → return system value (if any) before the parsed value.
        { name -> getSystemEnv(name) }
    }

    val cmdExecutor: (String) -> String = { cmd -> executeCommand(cmd) }

    val parsed = DotenvParser(
        content = content,
        format = cfg.format,
        enableCommandSubstitution = cfg.enableCommandSubstitution,
        systemEnvLookup = systemLookup,
        executeCommand = cmdExecutor,
    ).parse()

    // Apply override policy to the final entry set.
    val entries: Map<String, String> = if (cfg.overrideSystemEnv) {
        parsed
    } else {
        // Exclude entries whose key is already present in the system environment.
        parsed.filter { (key, _) -> getSystemEnv(key) == null }
    }

    return Dotenv(entries)
}
