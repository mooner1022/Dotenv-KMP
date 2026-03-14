package dev.mooner.dotenv

import dev.mooner.dotenv.internal.parser.DotenvParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OriginalFormatParserTest {

    private fun parse(content: String): Map<String, String> =
        DotenvParser(content = content, format = DotenvFormat.Original).parse()

    // -------------------------------------------------------------------------
    // Basic assignments
    // -------------------------------------------------------------------------

    @Test
    fun basicKeyValue() {
        val result = parse("KEY=value")
        assertEquals("value", result["KEY"])
    }

    @Test
    fun emptyValue() {
        val result = parse("KEY=")
        assertEquals("", result["KEY"])
    }

    @Test
    fun multipleEntries() {
        val result = parse("A=1\nB=2\nC=3")
        assertEquals("1", result["A"])
        assertEquals("2", result["B"])
        assertEquals("3", result["C"])
    }

    @Test
    fun windowsLineEndings() {
        val result = parse("A=1\r\nB=2\r\n")
        assertEquals("1", result["A"])
        assertEquals("2", result["B"])
    }

    // -------------------------------------------------------------------------
    // Export prefix
    // -------------------------------------------------------------------------

    @Test
    fun exportPrefixIsIgnored() {
        val result = parse("export MY_VAR=hello")
        assertEquals("hello", result["MY_VAR"])
    }

    @Test
    fun exportPrefixWithQuotedValue() {
        val result = parse("export QUOTED=\"world\"")
        assertEquals("world", result["QUOTED"])
    }

    // -------------------------------------------------------------------------
    // Comments
    // -------------------------------------------------------------------------

    @Test
    fun fullLineComment() {
        val result = parse("# this is a comment\nKEY=value")
        assertNull(result["# this is a comment"])
        assertEquals("value", result["KEY"])
    }

    @Test
    fun inlineCommentAfterUnquotedValue() {
        val result = parse("KEY=hello # comment")
        assertEquals("hello", result["KEY"])
    }

    @Test
    fun inlineCommentAfterDoubleQuotedValue() {
        val result = parse("KEY=\"hello\" # comment")
        assertEquals("hello", result["KEY"])
    }

    @Test
    fun hashInsideDoubleQuotesIsNotAComment() {
        val result = parse("KEY=\"value#notacomment\"")
        assertEquals("value#notacomment", result["KEY"])
    }

    @Test
    fun hashInsideSingleQuotesIsNotAComment() {
        val result = parse("KEY='value#notacomment'")
        assertEquals("value#notacomment", result["KEY"])
    }

    @Test
    fun emptyLinesAreSkipped() {
        val result = parse("\n\n\nKEY=value\n\n")
        assertEquals("value", result["KEY"])
        assertEquals(1, result.size)
    }

    // -------------------------------------------------------------------------
    // Whitespace trimming
    // -------------------------------------------------------------------------

    @Test
    fun unquotedValueIsTrimmed() {
        val result = parse("KEY=   hello world   ")
        assertEquals("hello world", result["KEY"])
    }

    @Test
    fun leadingWhitespaceBeforeKeyIsSkipped() {
        val result = parse("   KEY=value")
        assertEquals("value", result["KEY"])
    }

    @Test
    fun whitespaceAroundEqualsSign() {
        val result = parse("KEY   =   value")
        // Spec: whitespace between key and '=' is skipped; unquoted value is trimmed.
        assertEquals("value", result["KEY"])
    }

    // -------------------------------------------------------------------------
    // Single-quoted values
    // -------------------------------------------------------------------------

    @Test
    fun singleQuotedValue() {
        val result = parse("KEY='hello world'")
        assertEquals("hello world", result["KEY"])
    }

    @Test
    fun singleQuotedPreservesInnerWhitespace() {
        val result = parse("KEY='  spaced  '")
        assertEquals("  spaced  ", result["KEY"])
    }

    @Test
    fun singleQuotedNoEscapeProcessing() {
        val result = parse("KEY='new\\nline'")
        assertEquals("new\\nline", result["KEY"])
    }

    @Test
    fun singleQuotedMultiline() {
        val result = parse("KEY='line1\nline2'")
        assertEquals("line1\nline2", result["KEY"])
    }

    // -------------------------------------------------------------------------
    // Double-quoted values
    // -------------------------------------------------------------------------

    @Test
    fun doubleQuotedValue() {
        val result = parse("KEY=\"hello\"")
        assertEquals("hello", result["KEY"])
    }

    @Test
    fun doubleQuotedPreservesInnerWhitespace() {
        val result = parse("KEY=\"  spaced  \"")
        assertEquals("  spaced  ", result["KEY"])
    }

    @Test
    fun doubleQuotedEscapeNewline() {
        val result = parse("KEY=\"line1\\nline2\"")
        assertEquals("line1\nline2", result["KEY"])
    }

    @Test
    fun doubleQuotedEscapeTab() {
        val result = parse("KEY=\"col1\\tcol2\"")
        assertEquals("col1\tcol2", result["KEY"])
    }

    @Test
    fun doubleQuotedEscapeCarriageReturn() {
        val result = parse("KEY=\"a\\rb\"")
        assertEquals("a\rb", result["KEY"])
    }

    @Test
    fun doubleQuotedEscapeBackslash() {
        val result = parse("KEY=\"back\\\\slash\"")
        assertEquals("back\\slash", result["KEY"])
    }

    @Test
    fun doubleQuotedEscapeQuote() {
        val result = parse("""KEY="say \"hi\""""")
        assertEquals("say \"hi\"", result["KEY"])
    }

    @Test
    fun doubleQuotedMultilineWithLiteralNewline() {
        val content = "KEY=\"line1\nline2\""
        val result = parse(content)
        assertEquals("line1\nline2", result["KEY"])
    }

    // -------------------------------------------------------------------------
    // Backtick-quoted values
    // -------------------------------------------------------------------------

    @Test
    fun backtickValue() {
        val result = parse("KEY=`hello world`")
        assertEquals("hello world", result["KEY"])
    }

    @Test
    fun backtickMultiline() {
        val result = parse("KEY=`line1\nline2`")
        assertEquals("line1\nline2", result["KEY"])
    }

    // -------------------------------------------------------------------------
    // No variable interpolation in Original format
    // -------------------------------------------------------------------------

    @Test
    fun dollarSignIsLiteralInOriginalFormat() {
        val result = parse("BASE=hello\nKEY=\${BASE}/world")
        // In Original format, ${BASE} is NOT expanded.
        assertEquals("\${BASE}/world", result["KEY"])
    }

    // -------------------------------------------------------------------------
    // Invalid / malformed lines are skipped
    // -------------------------------------------------------------------------

    @Test
    fun lineWithoutEqualsIsSkipped() {
        val result = parse("NOT_A_PAIR\nKEY=value")
        assertNull(result["NOT_A_PAIR"])
        assertEquals("value", result["KEY"])
    }

    @Test
    fun keyStartingWithDigitIsSkipped() {
        val result = parse("1INVALID=value\nKEY=ok")
        assertNull(result["1INVALID"])
        assertEquals("ok", result["KEY"])
    }

    @Test
    fun emptyInputReturnsEmptyMap() {
        assertTrue(parse("").isEmpty())
    }

    @Test
    fun allCommentsReturnsEmptyMap() {
        assertTrue(parse("# comment 1\n# comment 2").isEmpty())
    }
}
