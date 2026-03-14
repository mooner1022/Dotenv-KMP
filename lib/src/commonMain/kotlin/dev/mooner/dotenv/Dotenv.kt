package dev.mooner.dotenv

/**
 * Provides read access to key-value pairs parsed from a .env file.
 *
 * Create an instance via the [dotenv] DSL function:
 * ```kotlin
 * val env = dotenv {
 *     path   = ".env"
 *     format = DotenvFormat.Dotenvy
 * }
 * val port = env["PORT"] ?: "8080"
 * ```
 */
class Dotenv internal constructor(private val entries: Map<String, String>) {

    /**
     * Returns the value for [key], or `null` if it is absent.
     */
    operator fun get(key: String): String? = entries[key]

    /**
     * Returns the value for [key], or [default] if it is absent.
     */
    fun getOrDefault(key: String, default: String): String = entries[key] ?: default

    /**
     * Returns the value for [key], or throws [DotenvException.MissingKeyException] if absent.
     */
    fun getOrThrow(key: String): String =
        entries[key] ?: throw DotenvException.MissingKeyException(key)

    /**
     * Returns `true` if [key] is present.
     */
    operator fun contains(key: String): Boolean = key in entries

    /**
     * Returns a read-only snapshot of all key-value pairs.
     */
    fun toMap(): Map<String, String> = entries.toMap()

    /** The number of entries loaded from the .env file. */
    val size: Int get() = entries.size

    /** All keys present in the .env file. */
    val keys: Set<String> get() = entries.keys

    override fun toString(): String = "Dotenv(size=$size)"
}
