package dev.mooner.dotenv.internal.parser

import dev.mooner.dotenv.DotenvException
import dev.mooner.dotenv.DotenvFormat

internal class DotenvParser(
    private val content: String,
    private val format: DotenvFormat,
    private val enableCommandSubstitution: Boolean = false,
    private val systemEnvLookup: (String) -> String? = { null },
    private val executeCommand: (String) -> String = { cmd ->
        throw DotenvException.CommandSubstitutionException(cmd)
    },
) {
    private var pos = 0
    private var lineNum = 1

    // Accumulated key→value pairs, also used for self-referential interpolation.
    private val vars = mutableMapOf<String, String>()

    fun parse(): Map<String, String> {
        while (!isEof()) {
            skipHorizontalWhitespace()
            when {
                isEof()                    -> break
                curIs('\n') || curIs('\r') -> advanceLine()
                curIs('#')                 -> skipRestOfLine()
                else                       -> parseEntry()
            }
        }
        return vars.toMap()
    }

    private fun parseEntry() {
        // Optional 'export' prefix — the keyword is consumed and ignored.
        if (content.startsWith("export", pos) && pos + 6 < content.length) {
            val after = content[pos + 6]
            if (after == ' ' || after == '\t') {
                pos += 6
                skipHorizontalWhitespace()
            }
        }

        val key = parseKey() ?: run { skipRestOfLine(); return }
        skipHorizontalWhitespace()

        if (!curIs('=')) { skipRestOfLine(); return }
        pos++ // consume '='

        val value = parseValue()
        vars[key] = value

        // Trailing whitespace then optional inline '#' comment, then newline.
        skipHorizontalWhitespace()
        if (curIs('#')) skipRestOfLine()
        if (!isEof()) advanceLine()
    }

    private fun parseKey(): String? {
        if (isEof() || (!cur().isLetter() && cur() != '_')) return null
        val start = pos
        while (!isEof() && (cur().isLetterOrDigit() || cur() == '_')) pos++
        return content.substring(start, pos).takeIf { it.isNotEmpty() }
    }

    private fun parseValue(): String {
        if (isEof() || curIs('\n') || curIs('\r')) return ""

        // Skip any horizontal whitespace between '=' and the value so that
        // `KEY = value`, `KEY = "..."`, and `KEY = """..."""` all parse correctly.
        skipHorizontalWhitespace()

        if (isEof() || curIs('\n') || curIs('\r')) return ""

        // Dotenvy: triple-quoted heredoc takes priority over double-quote.
        if (format == DotenvFormat.Dotenvy && content.startsWith("\"\"\"", pos)) {
            return parseHeredoc()
        }

        return when {
            curIs('"')  -> parseDoubleQuoted()
            curIs('\'') -> parseSingleQuoted()
            curIs('`')  -> parseBacktick()
            else        -> parseUnquoted()
        }
    }

    private fun parseUnquoted(): String {
        val sb = StringBuilder()
        while (!isEof() && !curIs('\n') && !curIs('\r')) {
            when {
                curIs('#') -> break  // inline comment
                curIs('$') && format == DotenvFormat.Dotenvy -> {
                    pos++
                    sb.append(resolveInterpolation())
                }
                else -> sb.append(content[pos++])
            }
        }
        return sb.toString().trimEnd()
    }

    private fun parseSingleQuoted(): String {
        pos++ // opening '
        val sb = StringBuilder()
        while (!isEof() && !curIs('\'')) {
            if (curIs('\n')) lineNum++
            sb.append(content[pos++])
        }
        if (!isEof()) pos++ // closing '
        return sb.toString()
    }

    private fun parseDoubleQuoted(): String {
        pos++ // opening "
        val sb = StringBuilder()
        while (!isEof() && !curIs('"')) {
            when {
                curIs('\\') -> {
                    pos++
                    sb.append(parseEscape(extended = format == DotenvFormat.Dotenvy))
                }
                curIs('$') && format == DotenvFormat.Dotenvy -> {
                    pos++
                    sb.append(resolveInterpolation())
                }
                else -> {
                    if (curIs('\n')) lineNum++
                    sb.append(content[pos++])
                }
            }
        }
        if (!isEof()) pos++ // closing "
        return sb.toString()
    }

    private fun parseBacktick(): String {
        pos++ // opening `
        val sb = StringBuilder()
        while (!isEof() && !curIs('`')) {
            when {
                curIs('\\') -> {
                    pos++
                    sb.append(parseEscape(extended = false))
                }
                else -> {
                    if (curIs('\n')) lineNum++
                    sb.append(content[pos++])
                }
            }
        }
        if (!isEof()) pos++ // closing `
        return sb.toString()
    }

    private fun parseHeredoc(): String {
        pos += 3 // opening """
        // Skip one optional newline immediately after the opening triple-quote.
        if (!isEof() && curIs('\r')) pos++
        if (!isEof() && curIs('\n')) { lineNum++; pos++ }

        val sb = StringBuilder()
        while (!isEof()) {
            if (content.startsWith("\"\"\"", pos)) {
                pos += 3 // closing """
                break
            }
            if (curIs('\n')) lineNum++
            sb.append(content[pos++])
        }
        // Trim the trailing newline that precedes the closing """.
        return sb.toString().trimEnd('\n', '\r')
    }

    private fun parseEscape(extended: Boolean): String {
        if (isEof()) return "\\"
        return when (val c = content[pos++]) {
            'n'  -> "\n"
            'r'  -> "\r"
            't'  -> "\t"
            '\\' -> "\\"
            '"'  -> "\""
            '\'' -> "'"
            'f'  -> if (extended) "\u000C" else "\\f"
            'b'  -> if (extended) "\b"     else "\\b"
            'u'  -> if (extended) parseUnicodeEscape() else "u"
            else -> c.toString()  // unknown → pass character through
        }
    }

    private fun parseUnicodeEscape(): String {
        if (pos + 4 > content.length) return "u"
        val hex = content.substring(pos, pos + 4)
        val code = hex.toIntOrNull(16) ?: return "u$hex"
        pos += 4
        return code.toChar().toString()
    }

    private fun resolveInterpolation(): String {
        if (isEof()) return "$"
        return when {
            curIs('{') -> {
                pos++ // consume '{'
                val name = readUntilChar('}')
                if (!isEof()) pos++ // consume '}'
                lookupVar(name)
            }
            curIs('(') -> {
                pos++ // consume '('
                val command = readUntilChar(')')
                if (!isEof()) pos++ // consume ')'
                if (enableCommandSubstitution) {
                    try {
                        executeCommand(command).trimEnd('\n', '\r')
                    } catch (e: DotenvException.CommandSubstitutionException) {
                        throw e
                    } catch (e: Exception) {
                        throw DotenvException.CommandSubstitutionException(command, e)
                    }
                } else {
                    $$"$($${command})"  // leave unexpanded when substitution is disabled
                }
            }
            cur().isLetter() || cur() == '_' -> {
                val start = pos
                while (!isEof() && (cur().isLetterOrDigit() || cur() == '_')) pos++
                lookupVar(content.substring(start, pos))
            }
            else -> "$"
        }
    }

    private fun lookupVar(name: String): String =
        vars[name] ?: systemEnvLookup(name) ?: ""

    private fun readUntilChar(stop: Char): String {
        val start = pos
        while (!isEof() && !curIs(stop)) pos++
        return content.substring(start, pos)
    }

    private fun isEof(): Boolean = pos >= content.length
    private fun cur(): Char = content[pos]
    private fun curIs(c: Char): Boolean = !isEof() && content[pos] == c

    private fun skipHorizontalWhitespace() {
        while (!isEof() && (curIs(' ') || curIs('\t'))) pos++
    }

    private fun skipRestOfLine() {
        while (!isEof() && !curIs('\n') && !curIs('\r')) pos++
        if (!isEof()) advanceLine()
    }

    private fun advanceLine() {
        if (curIs('\r')) pos++
        if (curIs('\n')) pos++
        lineNum++
    }
}
