# Dotenv KMP

A Kotlin Multiplatform library for loading and parsing `.env` files. Supports two dialects: the widely-used **Original** format (compatible with [motdotla/dotenv](https://github.com/motdotla/dotenv)) and the extended **Dotenvy** format with variable interpolation and heredocs.

## Supported targets

| Target | File I/O | Command substitution |
|---|---|---|
| JVM | ✅ | ✅ |
| linuxX64 | ✅ | ✅ |
| macosX64 / macosArm64 | ✅ | ✅ |
| mingwX64 | ✅ | ❌ |

## Installation

```kotlin
// build.gradle.kts
dependencies {
    implementation("dev.mooner:dotenv-kmp:1.0.0")
}
```

## Quick start

```kotlin
val env = dotenv {
    path = ".env"
}

val host = env["DB_HOST"] ?: "localhost"
val port = env.getOrDefault("DB_PORT", "5432")
val secret = env.getOrThrow("SECRET_KEY") // throws if missing
```

## Configuration

All options are set inside the `dotenv { }` block:

| Property | Type | Default | Description |
|---|---|---|---|
| `path` | `String` | `".env"` | Path to the `.env` file |
| `format` | `DotenvFormat` | `Original` | Parsing dialect (`Original` or `Dotenvy`) |
| `overrideSystemEnv` | `Boolean` | `false` | When `true`, `.env` values take precedence over system environment variables |
| `ignoreIfMissing` | `Boolean` | `false` | When `true`, a missing file returns an empty `Dotenv` instead of throwing |
| `enableCommandSubstitution` | `Boolean` | `false` | When `true`, `$(command)` expressions are executed (Dotenvy only) |
| `content` | `String?` | `null` | Parse a string directly, bypassing all file I/O |

```kotlin
val env = dotenv {
    path = "/etc/myapp/.env"
    format = DotenvFormat.Dotenvy
    overrideSystemEnv = true
    ignoreIfMissing = true
}

// Parse from a string (useful in tests or when loading from a non-filesystem source)
val env = dotenv {
    content = "KEY=value\nOTHER=123"
}
```

## Accessing values

```kotlin
val env = dotenv()

env["KEY"]                           // String? — null if absent
env.getOrDefault("KEY", "fallback")  // String
env.getOrThrow("KEY")                // String — throws MissingKeyException if absent
"KEY" in env                         // Boolean
env.toMap()                          // Map<String, String>
env.size                             // Int
env.keys                             // Set<String>
```

## Formats

### Original

Compatible with [motdotla/dotenv](https://github.com/motdotla/dotenv). Use `DotenvFormat.Original` (default).

```sh
# Full-line comments
export EXPORT_PREFIX=ignored      # 'export' keyword is stripped

UNQUOTED=hello world              # trailing whitespace is trimmed
SINGLE='raw \n no escapes'        # single-quotes: completely literal
DOUBLE="line1\nline2"             # double-quotes: \n \r \t \\ \" supported
BACKTICK=`multi
line`                             # backtick: multiline, basic escapes

INLINE=value # this part ignored  # inline comments after values
```

**No variable interpolation** — `${}` and `$VAR` are kept as-is.

### Dotenvy

Extended format. Use `DotenvFormat.Dotenvy`.

Includes everything in Original, plus:

```sh
# Variable interpolation
BASE=/home/user
DERIVED=${BASE}/app               # → /home/user/app
SHORT=$BASE/app                   # same, without braces

# Self-referential / chained interpolation
A=first
B=${A}-second                     # → first-second

# Extended escape sequences (double-quoted only)
UNICODE="\u0041\u0042\u0043"      # → ABC
MISC="\f\b\'"

# Triple-quoted heredoc (raw — no escapes or interpolation)
SQL = """
  SELECT *
  FROM users
  WHERE active = 1
"""

# Command substitution (requires enableCommandSubstitution = true)
HOSTNAME=$(hostname)
```

Single-quoted values are always fully literal in both formats — no interpolation, no escapes.

## Exceptions

All exceptions extend `DotenvException`:

| Type | When thrown |
|---|---|
| `DotenvException.FileNotFoundException` | File not found and `ignoreIfMissing` is `false` |
| `DotenvException.ParseException` | Malformed input |
| `DotenvException.MissingKeyException` | `getOrThrow()` called for an absent key |
| `DotenvException.CommandSubstitutionException` | `$(command)` execution fails |

```kotlin
try {
    val env = dotenv { path = "missing.env" }
} catch (e: DotenvException.FileNotFoundException) {
    println("Not found: ${e.path}")
}
```

## License

[MIT](LICENSE)
