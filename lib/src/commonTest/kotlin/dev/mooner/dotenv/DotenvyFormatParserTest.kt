package dev.mooner.dotenv

import dev.mooner.dotenv.internal.parser.DotenvParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DotenvyFormatParserTest {

    private fun parse(
        content: String,
        systemEnv: Map<String, String> = emptyMap(),
        enableCommandSubstitution: Boolean = false,
        commandOutput: Map<String, String> = emptyMap(),
    ): Map<String, String> = DotenvParser(
        content = content,
        format = DotenvFormat.Dotenvy,
        enableCommandSubstitution = enableCommandSubstitution,
        systemEnvLookup = { systemEnv[it] },
        executeCommand = { cmd -> commandOutput[cmd] ?: "" },
    ).parse()

    // -------------------------------------------------------------------------
    // All Original features still work in Dotenvy mode
    // -------------------------------------------------------------------------

    @Test
    fun basicAssignmentStillWorks() {
        assertEquals("hello", parse("KEY=hello")["KEY"])
    }

    @Test
    fun exportPrefixStillIgnored() {
        assertEquals("world", parse("export KEY=world")["KEY"])
    }

    @Test
    fun commentLinesStillSkipped() {
        val result = parse("# ignore me\nKEY=value")
        assertEquals("value", result["KEY"])
        assertEquals(1, result.size)
    }

    @Test
    fun singleQuotedIsRaw() {
        assertEquals("no\\nescapes", parse("KEY='no\\nescapes'")["KEY"])
    }

    @Test
    fun doubleQuotedBasicEscapes() {
        assertEquals("a\nb", parse("KEY=\"a\\nb\"")["KEY"])
    }

    // -------------------------------------------------------------------------
    // Extended escape sequences (Dotenvy only)
    // -------------------------------------------------------------------------

    @Test
    fun extendedEscapeFormFeed() {
        val result = parse("KEY=\"a\\fb\"")
        assertEquals("a\u000Cb", result["KEY"])
    }

    @Test
    fun extendedEscapeBackspace() {
        val result = parse("KEY=\"a\\bb\"")
        assertEquals("a\bb", result["KEY"])
    }

    @Test
    fun extendedEscapeSingleQuoteInsideDoubleQuoted() {
        val result = parse("KEY=\"it\\'s\"")
        assertEquals("it's", result["KEY"])
    }

    @Test
    fun unicodeEscapeSequence() {
        val result = parse("KEY=\"\\u0048\\u0065\\u006C\\u006C\\u006F\"")
        assertEquals("Hello", result["KEY"])
    }

    @Test
    fun unicodeEscapeProducesCorrectChar() {
        val result = parse("KEY=\"\\u03B1\"")  // α
        assertEquals("α", result["KEY"])
    }

    // -------------------------------------------------------------------------
    // Variable interpolation  ${VAR}  and  $VAR
    // -------------------------------------------------------------------------

    @Test
    fun interpolationWithBracesInDoubleQuoted() {
        val result = parse("BASE=hello\nDERIVED=\"\${BASE} world\"")
        assertEquals("hello world", result["DERIVED"])
    }

    @Test
    fun interpolationWithBracesInUnquoted() {
        val result = parse("BASE=foo\nDERIVED=\${BASE}/bar")
        assertEquals("foo/bar", result["DERIVED"])
    }

    @Test
    fun interpolationWithoutBraces() {
        val result = parse("NAME=Kotlin\nGREET=Hello \$NAME!")
        assertEquals("Hello Kotlin!", result["GREET"])
    }

    @Test
    fun interpolationUsesEarlierParsedVar() {
        val result = parse("A=first\nB=\${A}-second\nC=\${B}-third")
        assertEquals("first-second-third", result["C"])
    }

    @Test
    fun interpolationFallsBackToSystemEnv() {
        val result = parse(
            content = "KEY=\${SYS_VAR}/path",
            systemEnv = mapOf("SYS_VAR" to "system"),
        )
        assertEquals("system/path", result["KEY"])
    }

    @Test
    fun undefinedVariableExpandsToEmpty() {
        val result = parse("KEY=prefix_\${UNDEFINED}_suffix")
        assertEquals("prefix__suffix", result["KEY"])
    }

    @Test
    fun singleQuotedPreventsInterpolation() {
        val result = parse("BASE=hello\nKEY='\${BASE} world'")
        // Single-quoted → literal, no expansion.
        assertEquals("\${BASE} world", result["KEY"])
    }

    @Test
    fun multipleDollarSignsInValue() {
        val result = parse("A=x\nKEY=\${A}+\${A}")
        assertEquals("x+x", result["KEY"])
    }

    // -------------------------------------------------------------------------
    // Triple-quoted heredoc
    // -------------------------------------------------------------------------

    @Test
    fun heredocBasic() {
        val content = """
            KEY = ${"\"\"\""}
            line 1
            line 2
            ${"\"\"\""}
        """.trimIndent()
        val result = parse(content)
        assertEquals("line 1\nline 2", result["KEY"])
    }

    @Test
    fun heredocNoEscapeProcessing() {
        val content = "KEY = \"\"\"\nno\\nescapes\n\"\"\""
        val result = parse(content)
        assertEquals("no\\nescapes", result["KEY"])
    }

    @Test
    fun heredocNoInterpolation() {
        val content = "BASE=x\nKEY = \"\"\"\n\${BASE}\n\"\"\""
        val result = parse(content)
        // Heredoc is raw — no interpolation.
        assertEquals("\${BASE}", result["KEY"])
    }

    @Test
    fun heredocPreservesInternalWhitespace() {
        val content = "KEY = \"\"\"\n  indented\n    more\n\"\"\""
        val result = parse(content)
        assertEquals("  indented\n    more", result["KEY"])
    }

    // -------------------------------------------------------------------------
    // Command substitution  $(command)  — disabled by default
    // -------------------------------------------------------------------------

    @Test
    fun commandSubstitutionDisabledByDefault() {
        // When disabled, the literal expression is kept in the value.
        val result = parse("KEY=prefix-\$(echo hello)-suffix")
        assertEquals("prefix-\$(echo hello)-suffix", result["KEY"])
    }

    @Test
    fun commandSubstitutionEnabledInUnquoted() {
        val result = parse(
            content = "KEY=\$(echo hello)",
            enableCommandSubstitution = true,
            commandOutput = mapOf("echo hello" to "hello"),
        )
        assertEquals("hello", result["KEY"])
    }

    @Test
    fun commandSubstitutionEnabledInDoubleQuoted() {
        val result = parse(
            content = "KEY=\"prefix-\$(echo world)\"",
            enableCommandSubstitution = true,
            commandOutput = mapOf("echo world" to "world"),
        )
        assertEquals("prefix-world", result["KEY"])
    }

    @Test
    fun commandSubstitutionDisabledInSingleQuoted() {
        val result = parse(
            content = "KEY='\$(echo never)'",
            enableCommandSubstitution = true,
            commandOutput = mapOf("echo never" to "never"),
        )
        // Single-quoted is always raw.
        assertEquals("\$(echo never)", result["KEY"])
    }

    // -------------------------------------------------------------------------
    // Edge cases
    // -------------------------------------------------------------------------

    @Test
    fun emptyFileReturnsEmptyMap() {
        assertTrue(parse("").isEmpty())
    }

    @Test
    fun interpolationOfEmptyString() {
        val result = parse("EMPTY=\nKEY=[\${EMPTY}]")
        assertEquals("[]", result["KEY"])
    }

    @Test
    fun dollarAtEndOfLine() {
        val result = parse("KEY=value\$")
        assertEquals("value\$", result["KEY"])
    }
}
