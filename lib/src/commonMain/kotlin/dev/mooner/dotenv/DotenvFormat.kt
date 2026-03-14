package dev.mooner.dotenv

/**
 * Selects the parsing dialect used when reading a .env file.
 */
enum class DotenvFormat {
    /**
     * Original dotenv format, compatible with motdotla/dotenv.
     *
     * Supported features:
     * - Basic `KEY=value` assignments
     * - Optional `export KEY=value` prefix (keyword is ignored)
     * - Line comments starting with `#`
     * - Inline `#` comments after any value
     * - Unquoted values: whitespace trimmed from both ends
     * - Single-quoted `'...'`: literal/raw, no escape processing
     * - Double-quoted `"..."`: escape sequences (`\n \r \t \\ \"`), multiline
     * - Backtick `` `...` ``: multiline, basic escape sequences
     * - Empty values: `KEY=` → empty string
     */
    Original,

    /**
     * Extended Dotenvy format. Includes all [Original] features, plus:
     * - Variable interpolation `${VAR}` in unquoted and double-quoted values
     * - Simple variable reference `$VAR` without braces
     * - Triple-quoted heredocs `"""..."""` (no escape or interpolation processing)
     * - Extended escape sequences: `\f \b \' \uXXXX` in double-quoted values
     * - Command substitution `$(command)` when [DotenvBuilder.enableCommandSubstitution] is true
     * - Single quotes strictly prevent ALL interpolation and substitution
     */
    Dotenvy,
}
