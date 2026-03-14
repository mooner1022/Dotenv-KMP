package dev.mooner.dotenv.app

import dev.mooner.dotenv.DotenvException
import dev.mooner.dotenv.DotenvFormat
import dev.mooner.dotenv.dotenv

fun main() {
    println("=== Dotenv-KMP Demo ===\n")

    // -------------------------------------------------------------------------
    // 1. Original format — parsed from an inline string
    // -------------------------------------------------------------------------
    println("-- Original format --")

    val originalContent = buildString {
        appendLine("# Application settings")
        appendLine("APP_NAME=Dotenv-KMP Demo")
        appendLine("APP_PORT=8080")
        appendLine("DEBUG=false")
        appendLine("export DB_HOST=\"localhost\"")
        appendLine("DB_PORT='5432'")
        appendLine("DB_NAME=mydb")
        // \n inside double-quoted value → literal newline
        appendLine("MULTILINE=\"line one\\nline two\\nline three\"")
        appendLine("EMPTY_VALUE=")
        appendLine("INLINE_COMMENT=hello # this part is a comment")
        appendLine("PADDED   =   trimmed value")
    }

    val original = dotenv {
        content = originalContent
        format  = DotenvFormat.Original
    }

    original.toMap().forEach { (k, v) ->
        println("  %-20s = %s".format(k, v.replace("\n", "\\n")))
    }

    println()

    // -------------------------------------------------------------------------
    // 2. Dotenvy format — parsed from an inline string
    // \$ in Kotlin string literals produces a literal '$' in the output,
    // which the Dotenvy parser then treats as an interpolation prefix.
    // -------------------------------------------------------------------------
    println("-- Dotenvy format --")

    val dotenvyContent = buildString {
        appendLine("BASE_URL=https://example.com")
        // \${BASE_URL} → literal ${BASE_URL} in content → interpolated by parser
        appendLine("API_URL=\${BASE_URL}/api/v1")
        // \uXXXX escapes are processed inside double-quoted Dotenvy values
        appendLine("UNICODE=\"\\u0048\\u0065\\u006C\\u006C\\u006F\"")
        // Single-quoted: ${…} is NOT expanded
        appendLine("RAW='No \${interpolation} here'")
        // Extended escapes: \t (tab) and \f (form feed)
        appendLine("SPECIAL=\"tab:\\there\\fform-feed\"")
    }

    val dotenvy = dotenv {
        content = dotenvyContent
        format  = DotenvFormat.Dotenvy
    }

    dotenvy.toMap().forEach { (k, v) ->
        val display = v
            .replace("\t", "\\t")
            .replace("\u000C", "\\f")
            .replace("\n", "\\n")
        println("  %-20s = %s".format(k, display))
    }

    println()

    // -------------------------------------------------------------------------
    // 3. File-based loading (ignores missing .env gracefully)
    // -------------------------------------------------------------------------
    println("-- File-based loading (.env in working directory) --")

    val fileBased = dotenv {
        path            = ".env"
        format          = DotenvFormat.Original
        ignoreIfMissing = true
    }

    if (fileBased.size == 0) {
        println("  (no .env file found in working directory)")
    } else {
        println("  Loaded ${fileBased.size} entries from .env")
        fileBased.toMap().forEach { (k, v) -> println("    $k = $v") }
    }

    println()

    // -------------------------------------------------------------------------
    // 4. Public API surface demonstration
    // -------------------------------------------------------------------------
    println("-- API surface --")

    val env = dotenv { content = "DEFINED=hello\nANOTHER=world" }

    println("  get(DEFINED)               = ${env["DEFINED"]}")
    println("  get(MISSING)               = ${env["MISSING"]}")
    println("  getOrDefault(MISSING)      = ${env.getOrDefault("MISSING", "fallback")}")
    println("  \"DEFINED\" in env           = ${"DEFINED" in env}")
    println("  \"MISSING\" in env           = ${"MISSING" in env}")
    println("  size                       = ${env.size}")
    println("  keys                       = ${env.keys}")

    try {
        env.getOrThrow("MISSING")
    } catch (e: DotenvException.MissingKeyException) {
        println("  getOrThrow(MISSING) threw  MissingKeyException(key='${e.key}')")
    }
}
