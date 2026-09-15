package com.ismartcoding.plain.lib.codeeditor

import com.ismartcoding.plain.lib.codeeditor.TokenKind.*
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

/**
 * Language rule sets mirroring the extension coverage of the legacy pathToAceMode mapping.
 * Rules are data: keep them small and ordered (most specific first).
 */
object Languages : SynchronizedObject() {
    private val cache = HashMap<String, RegexLexer>()

    fun lexerFor(id: String): RegexLexer? {
        if (id == "text") return null
        return synchronized(this) { cache.getOrPut(id) { build(id) } }
    }

    fun pathToLanguageId(path: String): String {
        val name = path.substringAfterLast('/').lowercase()
        val ext = name.substringAfterLast('.', "")
        return when (ext) {
            "js", "javascript", "mjs", "cjs", "ts", "tsx", "jsx" -> "javascript"
            "java" -> "java"
            "kt", "kts", "kotlin" -> "kotlin"
            "xml", "html", "htm", "xhtml", "jsp", "jspx", "php", "phtml", "volt", "twig", "svg" -> "xml"
            "css", "less", "scss", "sass" -> "css"
            "json" -> "json"
            "md", "markdown" -> "markdown"
            "sql" -> "sql"
            "txt", "text", "log", "cfg", "ini", "conf", "properties" -> "text"
            "c", "h", "cpp", "hpp", "cc", "hh" -> "c_cpp"
            "py", "python" -> "python"
            "rb", "ruby" -> "ruby"
            "pl", "perl" -> "perl"
            "groovy", "gradle" -> "groovy"
            "swift" -> "swift"
            "go", "golang" -> "golang"
            "rs", "rust" -> "rust"
            "dart" -> "dart"
            "yaml", "yml" -> "yaml"
            "sh", "bash", "zsh", "bat" -> "shell"
            "dockerfile" -> "dockerfile"
            "r" -> "r"
            else -> if (name.startsWith("dockerfile")) "dockerfile" else "text"
        }
    }

    private fun build(id: String): RegexLexer = when (id) {
        "kotlin" -> kotlinLang
        "java" -> javaLang
        "javascript" -> javascriptLang
        "json" -> jsonLang
        "xml" -> xmlLang
        "css" -> cssLang
        "markdown" -> markdownLang
        "sql" -> sqlLang
        "c_cpp" -> cCppLang
        "python" -> pythonLang
        "ruby" -> rubyLang
        "perl" -> perlLang
        "groovy" -> groovyLang
        "swift" -> swiftLang
        "golang" -> golangLang
        "rust" -> rustLang
        "dart" -> dartLang
        "yaml" -> yamlLang
        "shell" -> shellLang
        "dockerfile" -> dockerfileLang
        "r" -> rLang
        else -> error("unknown language $id")
    }

    private fun keywords(words: String): String = "\\b(?:$words)\\b"

    private val kotlinLang = language("kotlin") {
        state("root") {
            rule("\\/\\*", COMMENT, push = "block_comment")
            rule("\\/\\/.*", COMMENT)
            rule("\"\"\"", STRING, push = "triple_string")
            rule("\"(?:[^\"\\\\\\n]|\\\\.)*\"?", STRING)
            rule("'(?:[^'\\\\\\n]|\\\\.)'?", STRING)
            rule("\\b0[xXbBoO][0-9a-fA-F_]+[uUL]?\\b", NUMBER)
            rule("\\b\\d[\\d_]*\\.?[\\d_]*(?:[eE][+-]?\\d+)?[fFuUlL]*\\b", NUMBER)
            rule(keywords("package|import|class|object|interface|fun|val|var|when|if|else|for|while|do|return|break|continue|is|as|in|by|where|constructor|init|companion|this|super|null|true|false|try|catch|finally|throw|typealias|data|sealed|enum|abstract|open|final|override|internal|private|protected|public|inline|crossinline|noinline|reified|operator|infix|suspend|tailrec|external|const|lateinit|vararg|out|expect|actual|annotation|funinterface|dynamic"), KEYWORD)
            rule("\\b(?:Int|Long|Short|Byte|Double|Float|Char|Boolean|String|Unit|Any|Nothing|List|Map|Set|Array|MutableList|MutableMap|MutableSet|Sequence|IntRange|Pair|Triple)\\b", TYPE)
            rule("@[A-Za-z_][A-Za-z0-9_]*(?:\\.[A-Za-z_][A-Za-z0-9_]*)*", ANNOTATION)
            rule("\\b[A-Z][A-Za-z0-9_]*\\b", TYPE)
            rule("\\b[a-z_][A-Za-z0-9_]*(?=\\s*\\()", FUNCTION)
        }
        state("block_comment") {
            rule("\\*\\/", COMMENT, pop = true)
            rule("[^*]+", COMMENT)
            rule("\\*", COMMENT)
        }
        state("triple_string") {
            rule("\"\"\"", STRING, pop = true)
            rule("[^\"]+", STRING)
            rule("\"(?!\"\")", STRING)
        }
    }

    private val javaLang = language("java") {
        state("root") {
            rule("\\/\\*", COMMENT, push = "block_comment")
            rule("\\/\\/.*", COMMENT)
            rule("\"(?:[^\"\\\\\\n]|\\\\.)*\"?", STRING)
            rule("'(?:[^'\\\\\\n]|\\\\.)'?", STRING)
            rule("\\b0[xX][0-9a-fA-F_]+[lL]?\\b", NUMBER)
            rule("\\b\\d[\\d_]*\\.?[\\d_]*(?:[eE][+-]?\\d+)?[fFdDlL]*\\b", NUMBER)
            rule(keywords("package|import|class|interface|enum|record|extends|implements|new|public|private|protected|static|final|abstract|synchronized|volatile|transient|native|strictfp|if|else|for|while|do|switch|case|default|break|continue|return|throw|throws|try|catch|finally|instanceof|this|super|null|true|false|var|yield|sealed|permits"), KEYWORD)
            rule("\\b(?:void|int|long|short|byte|double|float|char|boolean|String|Object|Integer|Long|Double|Float|Boolean|Character|Byte|Short|List|Map|Set|Optional)\\b", TYPE)
            rule("\\b[A-Z][A-Za-z0-9_]*\\b", TYPE)
            rule("@[A-Za-z_][A-Za-z0-9_]*(?:\\.[A-Za-z_][A-Za-z0-9_]*)*", ANNOTATION)
            rule("\\b[a-z_][A-Za-z0-9_]*(?=\\s*\\()", FUNCTION)
        }
        state("block_comment") {
            rule("\\*\\/", COMMENT, pop = true)
            rule("[^*]+", COMMENT)
            rule("\\*", COMMENT)
        }
    }

    private val javascriptLang = language("javascript") {
        state("root") {
            rule("\\/\\*", COMMENT, push = "block_comment")
            rule("\\/\\/.*", COMMENT)
            rule("`", STRING, push = "template_string")
            rule("\"(?:[^\"\\\\\\n]|\\\\.)*\"?", STRING)
            rule("'(?:[^'\\\\\\n]|\\\\.)'?", STRING)
            rule("\\b0[xXbBoO][0-9a-fA-F_]+n?\\b", NUMBER)
            rule("\\b\\d[\\d_]*\\.?[\\d_]*(?:[eE][+-]?\\d+)?n?\\b", NUMBER)
            rule(keywords("function|return|if|else|for|while|do|switch|case|default|break|continue|new|delete|typeof|instanceof|in|of|var|let|const|class|extends|super|this|null|undefined|true|false|async|await|yield|throw|try|catch|finally|static|get|set|import|export|from|as|void|interface|type|enum|implements|public|private|protected|readonly|declare|namespace|abstract|satisfies"), KEYWORD)
            rule("\\b(?:string|number|boolean|any|unknown|never|object|symbol|bigint)\\b", TYPE)
            rule("\\b[A-Z][A-Za-z0-9_$]*\\b", TYPE)
            rule("\\b[a-z_$][A-Za-z0-9_$]*(?=\\s*\\()", FUNCTION)
        }
        state("block_comment") {
            rule("\\*\\/", COMMENT, pop = true)
            rule("[^*]+", COMMENT)
            rule("\\*", COMMENT)
        }
        state("template_string") {
            rule("`", STRING, pop = true)
            rule("\\$\\{[^}]*\\}", CONSTANT)
            rule("[^`$]+", STRING)
            rule("\\$", STRING)
        }
    }

    private val jsonLang = language("json") {
        state("root") {
            rule("\"(?:[^\"\\\\]|\\\\.)*\"(?=\\s*:)", ATTRIBUTE)
            rule("\"(?:[^\"\\\\]|\\\\.)*\"", STRING)
            rule("-?\\b\\d+\\.?\\d*(?:[eE][+-]?\\d+)?\\b", NUMBER)
            rule("\\b(?:true|false|null)\\b", CONSTANT)
        }
    }

    private val xmlLang = language("xml") {
        state("root") {
            rule("<!--", COMMENT, push = "comment")
            rule("<\\?[\\w:-]+", TAG, push = "processing")
            rule("<!\\[CDATA\\[", CONSTANT, push = "cdata")
            rule("<\\/[\\w:.-]+>", TAG)
            rule("<[\\w:.-]+", TAG, push = "tag")
            rule("&[a-zA-Z0-9#]+;", CONSTANT)
        }
        state("comment") {
            rule("-->", COMMENT, pop = true)
            rule("[^-]+", COMMENT)
            rule("-", COMMENT)
        }
        state("cdata") {
            rule("]]>", CONSTANT, pop = true)
            rule("[^\\]]+", CONSTANT)
            rule("\\]", CONSTANT)
        }
        state("processing") {
            rule("\\?>", TAG, pop = true)
            rule("[^?]+", TAG)
            rule("\\?", TAG)
        }
        state("tag") {
            rule("\\s[\\w:.-]+(?=\\s*=)", ATTRIBUTE)
            rule("=\"[^\"]*\"", STRING)
            rule("='[^']*'", STRING)
            rule("\\/>", TAG, pop = true)
            rule(">", TAG, pop = true)
            rule("[\\w:.-]+", TAG)
        }
    }

    private val cssLang = language("css") {
        state("root") {
            rule("\\/\\*", COMMENT, push = "block_comment")
            rule("\"[^\"\\n]*\"", STRING)
            rule("'[^'\\n]*'", STRING)
            rule("@[\\w-]+", ANNOTATION)
            rule("#[0-9a-fA-F]{3,8}\\b", NUMBER)
            rule("\\b\\d+(?:\\.\\d+)?(?:px|em|rem|vh|vw|vmin|vmax|%|s|ms|deg|fr|ch|pt)?\\b", NUMBER)
            rule("\\.[A-Za-z_][\\w-]*", ATTRIBUTE)
            rule("::?[\\w-]+", ATTRIBUTE)
            rule("\\b[a-z-]+(?=\\s*:)", ATTRIBUTE)
            rule("[.#]?[\\w-]+(?=\\s*\\{)", TAG)
            rule("\\b(?:important|from|to)\\b", KEYWORD)
        }
        state("block_comment") {
            rule("\\*\\/", COMMENT, pop = true)
            rule("[^*]+", COMMENT)
            rule("\\*", COMMENT)
        }
    }

    private val markdownLang = language("markdown") {
        state("root") {
            rule("#{1,6} .*", TAG)
            rule("\\*\\*[^*\\n]+\\*\\*|__[^_\\n]+__", TYPE)
            rule("\\*[^*\\n]+\\*|_[^_\\n]+_", TYPE)
            rule("`+[^`\\n]*`+", STRING)
            rule(">.*", COMMENT)
            rule("!?\\[[^\\]\\n]*\\]\\([^)\\n]*\\)", ATTRIBUTE)
            rule("\\s[-=]{3,}\\s*$", COMMENT)
            rule("^\\s*(?:[-*+]|\\d+\\.)\\s", KEYWORD)
        }
    }

    private val sqlLang = language("sql") {
        state("root") {
            rule("--.*", COMMENT)
            rule("\\/\\*", COMMENT, push = "block_comment")
            rule("'(?:[^'\\\\]|\\\\.)*'?", STRING)
            rule("\\b\\d+(?:\\.\\d+)?\\b", NUMBER)
            rule("(?i)" + keywords("SELECT|FROM|WHERE|INSERT|INTO|VALUES|UPDATE|SET|DELETE|CREATE|TABLE|DROP|ALTER|ADD|INDEX|VIEW|JOIN|LEFT|RIGHT|INNER|OUTER|FULL|CROSS|ON|AS|AND|OR|NOT|NULL|IS|IN|LIKE|BETWEEN|EXISTS|GROUP|BY|ORDER|HAVING|LIMIT|OFFSET|DISTINCT|UNION|ALL|CASE|WHEN|THEN|ELSE|END|PRIMARY|KEY|FOREIGN|REFERENCES|CONSTRAINT|DEFAULT|UNIQUE|CHECK|CASCADE|BEGIN|COMMIT|ROLLBACK|TRANSACTION|WITH|RECURSIVE|ASC|DESC|COUNT|SUM|AVG|MIN|MAX|IF|NULLIF|COALESCE|CAST|EXPLAIN|VACUUM|ANALYZE|PRAGMA|ATTACH|DETACH"), KEYWORD)
            rule("(?i)\\b(?:INTEGER|TEXT|REAL|BLOB|NUMERIC|VARCHAR|CHAR|BOOLEAN|DATE|TIME|DATETIME|TIMESTAMP|INT|BIGINT|SMALLINT|FLOAT|DOUBLE|DECIMAL)\\b", TYPE)
        }
        state("block_comment") {
            rule("\\*\\/", COMMENT, pop = true)
            rule("[^*]+", COMMENT)
            rule("\\*", COMMENT)
        }
    }

    private val cCppLang = language("c_cpp") {
        state("root") {
            rule("\\/\\*", COMMENT, push = "block_comment")
            rule("\\/\\/.*", COMMENT)
            rule("\"(?:[^\"\\\\\\n]|\\\\.)*\"?", STRING)
            rule("'(?:[^'\\\\\\n]|\\\\.)'?", STRING)
            rule("\\b0[xXbB][0-9a-fA-F]+[uUlL]*\\b", NUMBER)
            rule("\\b\\d[\\d']*\\.?[\\d']*(?:[eE][+-]?\\d+)?[fFuUlL]*\\b", NUMBER)
            rule("^\\s*#\\s*\\w+", ANNOTATION)
            rule(keywords("if|else|for|while|do|switch|case|default|break|continue|return|goto|struct|class|union|enum|typedef|using|namespace|template|typename|new|delete|this|virtual|override|final|public|private|protected|friend|inline|static|const|constexpr|volatile|extern|register|thread_local|mutable|operator|sizeof|alignof|static_assert|decltype|nullptr|true|false|try|catch|throw|noexcept|concept|requires|co_await|co_return|co_yield|export|module|import"), KEYWORD)
            rule("\\b(?:void|bool|char|short|int|long|float|double|signed|unsigned|size_t|ssize_t|int8_t|int16_t|int32_t|int64_t|uint8_t|uint16_t|uint32_t|uint64_t|auto|wstring|string|string_view|vector|map|set|array|span|optional|variant)\\b", TYPE)
            rule("\\b[A-Z][A-Za-z0-9_]*\\b", TYPE)
            rule("\\b[a-z_][A-Za-z0-9_]*(?=\\s*\\()", FUNCTION)
        }
        state("block_comment") {
            rule("\\*\\/", COMMENT, pop = true)
            rule("[^*]+", COMMENT)
            rule("\\*", COMMENT)
        }
    }

    private val pythonLang = language("python") {
        state("root") {
            rule("#.*", COMMENT)
            rule("[rRbBfu]?\"\"\"", STRING, push = "triple_dq")
            rule("[rRbBfu]?'''", STRING, push = "triple_sq")
            rule("[rRbBfu]?\"(?:[^\"\\\\\\n]|\\\\.)*\"?", STRING)
            rule("[rRbBfu]?'(?:[^'\\\\\\n]|\\\\.)'?", STRING)
            rule("\\b0[xXoObB][0-9a-fA-F_]+\\b", NUMBER)
            rule("\\b\\d+\\.?\\d*(?:[eE][+-]?\\d+)?j?\\b", NUMBER)
            rule("@[\\w.]+", ANNOTATION)
            rule(keywords("def|class|return|if|elif|else|for|while|break|continue|pass|import|from|as|with|try|except|finally|raise|assert|yield|lambda|global|nonlocal|del|in|is|not|and|or|None|True|False|async|await|match|case|self|cls"), KEYWORD)
            rule("\\b(?:int|float|str|bool|bytes|list|dict|set|tuple|object|type|any|Optional|Union|Callable)\\b", TYPE)
            rule("\\b[A-Z][A-Za-z0-9_]*\\b", TYPE)
            rule("\\b[a-z_][A-Za-z0-9_]*(?=\\s*\\()", FUNCTION)
        }
        state("triple_dq") {
            rule("\"\"\"", STRING, pop = true)
            rule("[^\"]+", STRING)
            rule("\"(?!\"\")", STRING)
        }
        state("triple_sq") {
            rule("'''", STRING, pop = true)
            rule("[^']+", STRING)
            rule("'(?!'')", STRING)
        }
    }

    private val rubyLang = language("ruby") {
        state("root") {
            rule("#.*", COMMENT)
            rule("^=begin", COMMENT, push = "block_comment")
            rule("<<~?[A-Z_][A-Z0-9_]*", STRING)
            rule("\"(?:#\\{[^}]*\\}|[^\"\\\\]|\\\\.)*\"?", STRING)
            rule("'[^'\\n]*'", STRING)
            rule("\\b\\d+_?\\d*\\.?\\d*(?:[eE][+-]?\\d+)?\\b", NUMBER)
            rule(":[A-Za-z_][A-Za-z0-9_]*", CONSTANT)
            rule("\\$[A-Za-z_][A-Za-z0-9_]*|@[A-Za-z_][A-Za-z0-9_]*", VARIABLE)
            rule(keywords("def|end|class|module|if|elsif|else|unless|case|when|then|for|while|until|do|begin|rescue|ensure|raise|return|yield|next|break|redo|retry|and|or|not|in|new|self|nil|true|false|require|require_relative|include|extend|attr_accessor|attr_reader|attr_writer|private|protected|public|lambda|proc|puts|p|print"), KEYWORD)
            rule("\\b[A-Z][A-Za-z0-9_]*\\b", TYPE)
            rule("\\b[a-z_][A-Za-z0-9_?!]*(?=\\s*\\()", FUNCTION)
        }
        state("block_comment") {
            rule("^=end.*", COMMENT, pop = true)
            rule("[^=]+", COMMENT)
            rule("=", COMMENT)
        }
    }

    private val perlLang = language("perl") {
        state("root") {
            rule("#.*", COMMENT)
            rule("^=\\w+", COMMENT, push = "pod")
            rule("\"(?:[^\"\\\\]|\\\\.)*\"?", STRING)
            rule("'[^'\\n]*'", STRING)
            rule("\\b\\d+\\.?\\d*(?:[eE][+-]?\\d+)?\\b", NUMBER)
            rule("\\$[A-Za-z0-9_]+|@[A-Za-z0-9_]+|%[A-Za-z0-9_]+", VARIABLE)
            rule(keywords("sub|my|our|local|use|no|package|if|elsif|else|unless|while|until|for|foreach|do|last|next|redo|return|die|warn|print|printf|sprintf|eval|require|and|or|not|eq|ne|lt|gt|le|ge|cmp|undef|defined|exists|delete|keys|values|each|sort|map|grep|shift|unshift|push|pop|chomp|chop|split|join|open|close|read|write|bless|ref"), KEYWORD)
            rule("\\b[A-Z][A-Za-z0-9_]*\\b", TYPE)
        }
        state("pod") {
            rule("^=cut.*", COMMENT, pop = true)
            rule("[^=]+", COMMENT)
            rule("=", COMMENT)
        }
    }

    private val groovyLang = language("groovy") {
        state("root") {
            rule("\\/\\*", COMMENT, push = "block_comment")
            rule("\\/\\/.*", COMMENT)
            rule("'''", STRING, push = "triple_sq")
            rule("\"\"\"", STRING, push = "triple_dq")
            rule("\"(?:[^\"\\\\\\n]|\\\\.)*\"?", STRING)
            rule("'[^'\\n]*'", STRING)
            rule("\\$[A-Za-z_][A-Za-z0-9_]*", VARIABLE)
            rule("\\b\\d[\\d_]*\\.?[\\d_]*[fFdDgGlL]?\\b", NUMBER)
            rule(keywords("def|class|interface|enum|trait|extends|implements|new|package|import|static|final|var|if|else|for|while|switch|case|default|break|continue|return|try|catch|finally|throw|throws|this|super|null|true|false|in|as|instanceof|it|delegate|owner|closure|void|with|apply|plugins|dependencies|repositories|task|android|buildscript|ext"), KEYWORD)
            rule("\\b(?:int|long|short|byte|double|float|char|boolean|String|Object|List|Map|Set|Closure)\\b", TYPE)
            rule("\\b[A-Z][A-Za-z0-9_]*\\b", TYPE)
            rule("\\b[a-z_][A-Za-z0-9_]*(?=\\s*\\()", FUNCTION)
        }
        state("block_comment") {
            rule("\\*\\/", COMMENT, pop = true)
            rule("[^*]+", COMMENT)
            rule("\\*", COMMENT)
        }
        state("triple_dq") {
            rule("\"\"\"", STRING, pop = true)
            rule("[^\"]+", STRING)
            rule("\"(?!\"\")", STRING)
        }
        state("triple_sq") {
            rule("'''", STRING, pop = true)
            rule("[^']+", STRING)
            rule("'(?!'')", STRING)
        }
    }

    private val swiftLang = language("swift") {
        state("root") {
            rule("\\/\\*", COMMENT, push = "block_comment")
            rule("\\/\\/.*", COMMENT)
            rule("\"\"\"", STRING, push = "triple_string")
            rule("\"(?:[^\"\\\\\\n]|\\\\.)*\"?", STRING)
            rule("\\b0[xXoObB][0-9a-fA-F_]+\\b", NUMBER)
            rule("\\b\\d[\\d_]*\\.?[\\d_]*(?:[eE][+-]?\\d+)?\\b", NUMBER)
            rule("@[A-Za-z_][A-Za-z0-9_]*", ANNOTATION)
            rule(keywords("func|class|struct|enum|protocol|extension|import|let|var|if|else|guard|for|while|repeat|switch|case|default|break|continue|return|throw|throws|try|catch|defer|in|where|as|is|nil|true|false|self|Self|init|deinit|subscript|operator|typealias|associatedtype|some|any|async|await|actor|public|private|fileprivate|internal|open|final|override|mutating|nonmutating|lazy|weak|unowned|required|convenience|static|indirect|inout|rethrows|willSet|didSet|get|set|none"), KEYWORD)
            rule("\\b(?:Int|Double|Float|Bool|String|Character|Array|Dictionary|Set|Optional|Void|Never|Any|AnyObject|Result|Range|ClosedRange)\\b", TYPE)
            rule("\\b[A-Z][A-Za-z0-9_]*\\b", TYPE)
            rule("\\b[a-z_][A-Za-z0-9_]*(?=\\s*\\()", FUNCTION)
        }
        state("block_comment") {
            rule("\\*\\/", COMMENT, pop = true)
            rule("[^*]+", COMMENT)
            rule("\\*", COMMENT)
        }
        state("triple_string") {
            rule("\"\"\"", STRING, pop = true)
            rule("[^\"]+", STRING)
            rule("\"(?!\"\")", STRING)
        }
    }

    private val golangLang = language("golang") {
        state("root") {
            rule("\\/\\*", COMMENT, push = "block_comment")
            rule("\\/\\/.*", COMMENT)
            rule("`[^`]*`", STRING)
            rule("\"(?:[^\"\\\\\\n]|\\\\.)*\"?", STRING)
            rule("'(?:[^'\\\\\\n]|\\\\.)'?", STRING)
            rule("\\b0[xX][0-9a-fA-F_]+\\b", NUMBER)
            rule("\\b\\d[\\d_]*\\.?[\\d_]*(?:[eE][+-]?\\d+)?\\b", NUMBER)
            rule(keywords("package|import|func|type|struct|interface|map|chan|go|defer|select|switch|case|default|if|else|for|range|break|continue|return|goto|fallthrough|var|const|new|make|len|cap|append|copy|delete|panic|recover|nil|true|false|iota"), KEYWORD)
            rule("\\b(?:string|int|int8|int16|int32|int64|uint|uint8|uint16|uint32|uint64|uintptr|byte|rune|float32|float64|complex64|complex128|bool|error|any)\\b", TYPE)
            rule("\\b[A-Z][A-Za-z0-9_]*\\b", TYPE)
            rule("\\b[a-z_][A-Za-z0-9_]*(?=\\s*\\()", FUNCTION)
        }
        state("block_comment") {
            rule("\\*\\/", COMMENT, pop = true)
            rule("[^*]+", COMMENT)
            rule("\\*", COMMENT)
        }
    }

    private val rustLang = language("rust") {
        state("root") {
            rule("\\/\\*", COMMENT, push = "block_comment")
            rule("\\/\\/.*", COMMENT)
            rule("\"(?:[^\"\\\\]|\\\\.)*\"?", STRING)
            rule("r#*\"", STRING, push = "raw_string")
            rule("b\"(?:[^\"\\\\]|\\\\.)*\"?", STRING)
            rule("\\b\\d[\\d_]*(?:\\.\\d[\\d_]*)?(?:[eE][+-]?\\d+)?(?:f32|f64|i8|i16|i32|i64|i128|isize|u8|u16|u32|u64|u128|usize)?\\b", NUMBER)
            rule("\\b0x[0-9a-fA-F_]+(?:u8|u16|u32|u64|u128|usize|i8|i16|i32|i64|i128|isize)?\\b", NUMBER)
            rule("#!?\\[[^\\]]*\\]", ANNOTATION)
            rule("\\b[a-z_][A-Za-z0-9_]*!", KEYWORD)
            rule(keywords("fn|let|mut|const|static|struct|enum|trait|impl|for|in|if|else|match|while|loop|break|continue|return|use|mod|pub|crate|self|Self|super|as|dyn|ref|move|async|await|unsafe|extern|type|where|union|true|false"), KEYWORD)
            rule("\\b(?:u8|u16|u32|u64|u128|usize|i8|i16|i32|i64|i128|isize|f32|f64|bool|char|str|String|Vec|Option|Result|Box|Rc|Arc|HashMap|HashSet|BTreeMap)\\b", TYPE)
            rule("\\b[A-Z][A-Za-z0-9_]*\\b", TYPE)
            rule("\\b[a-z_][A-Za-z0-9_]*(?=\\s*\\()", FUNCTION)
        }
        state("block_comment") {
            rule("\\*\\/", COMMENT, pop = true)
            rule("[^*]+", COMMENT)
            rule("\\*", COMMENT)
        }
        state("raw_string") {
            rule("\"#+", STRING, pop = true)
            rule("[^\"#]+", STRING)
            rule("[\"#]", STRING)
        }
    }

    private val dartLang = language("dart") {
        state("root") {
            rule("\\/\\*", COMMENT, push = "block_comment")
            rule("\\/\\/.*", COMMENT)
            rule("'''", STRING, push = "triple_sq")
            rule("\"\"\"", STRING, push = "triple_dq")
            rule("\"(?:[^\"\\\\\\n]|\\\\.)*\"?", STRING)
            rule("'[^'\\n]*'", STRING)
            rule("\\$[A-Za-z_][A-Za-z0-9_]*", VARIABLE)
            rule("\\b\\d[\\d_]*\\.?[\\d_]*(?:[eE][+-]?\\d+)?\\b", NUMBER)
            rule("@[A-Za-z_][A-Za-z0-9_.]*", ANNOTATION)
            rule(keywords("abstract|as|assert|async|await|break|case|catch|class|const|continue|covariant|default|deferred|do|dynamic|else|enum|export|extends|extension|external|factory|false|final|finally|for|get|hide|if|implements|import|in|is|late|library|mixin|new|null|on|operator|part|required|rethrow|return|set|show|static|super|switch|sync|this|throw|true|try|typedef|var|void|while|with|yield"), KEYWORD)
            rule("\\b(?:int|double|num|String|bool|List|Map|Set|Iterable|Future|Stream|Object|Symbol|Type)\\b", TYPE)
            rule("\\b[A-Z][A-Za-z0-9_]*\\b", TYPE)
            rule("\\b[a-z_][A-Za-z0-9_]*(?=\\s*\\()", FUNCTION)
        }
        state("block_comment") {
            rule("\\*\\/", COMMENT, pop = true)
            rule("[^*]+", COMMENT)
            rule("\\*", COMMENT)
        }
        state("triple_dq") {
            rule("\"\"\"", STRING, pop = true)
            rule("[^\"]+", STRING)
            rule("\"(?!\"\")", STRING)
        }
        state("triple_sq") {
            rule("'''", STRING, pop = true)
            rule("[^']+", STRING)
            rule("'(?!'')", STRING)
        }
    }

    private val yamlLang = language("yaml") {
        state("root") {
            rule("#.*", COMMENT)
            rule("^---\\s*$|^\\.\\.\\.\\s*$", KEYWORD)
            rule("^\\s*-\\s", KEYWORD)
            rule("\\b(?:true|false|null|yes|no|on|off)\\b", CONSTANT)
            rule("\\b\\d+\\.?\\d*\\b", NUMBER)
            rule("\"[^\"\\n]*\"", STRING)
            rule("'[^'\\n]*'", STRING)
            rule("^\\s*[\\w.$-]+(?=\\s*:)", ATTRIBUTE)
            rule("&[\\w-]+|\\*[\\w-]+", ANNOTATION)
            rule("\\{[^}]*\\}|\\[[^]]*\\]", CONSTANT)
        }
    }

    private val shellLang = language("shell") {
        state("root") {
            rule("#.*", COMMENT)
            rule("\"[^\"\\n]*\"?", STRING)
            rule("'[^']*'", STRING)
            rule("\\$\\{[^}]*\\}|\\$[A-Za-z_][A-Za-z0-9_]*|\\$[0-9@?#$!*-]", VARIABLE)
            rule("\\b\\d+\\b", NUMBER)
            rule("^\\s*(?:function\\s+)?[\\w.-]+\\s*\\(\\)", FUNCTION)
            rule(keywords("if|then|elif|else|fi|for|in|do|done|while|until|case|esac|function|return|break|continue|local|export|readonly|declare|unset|shift|source|alias|exit|trap|set|eval|exec|echo|printf|cd|pwd|read|test|true|false"), KEYWORD)
        }
    }

    private val dockerfileLang = language("dockerfile") {
        state("root") {
            rule("#.*", COMMENT)
            rule("^\\s*(?:ONBUILD\\s+)?(?:FROM|RUN|CMD|LABEL|MAINTAINER|EXPOSE|ENV|ADD|COPY|ENTRYPOINT|VOLUME|USER|WORKDIR|ARG|ONBUILD|STOPSIGNAL|HEALTHCHECK|SHELL)\\b", KEYWORD)
            rule("--[\\w-]+", ATTRIBUTE)
            rule("\"[^\"\\n]*\"", STRING)
            rule("\\$\\{[^}]*\\}|\\$[A-Za-z_][A-Za-z0-9_]*", VARIABLE)
            rule("\\b[\\w./:-]+@[\\w.:-]+", CONSTANT)
        }
    }

    private val rLang = language("r") {
        state("root") {
            rule("#.*", COMMENT)
            rule("\"[^\"\\n]*\"", STRING)
            rule("'[^'\\n]*'", STRING)
            rule("\\b\\d+\\.?\\d*(?:[eE][+-]?\\d+)?L?\\b", NUMBER)
            rule("\\$[A-Za-z_.][A-Za-z0-9_.]*|@[A-Za-z_.][A-Za-z0-9_.]*", VARIABLE)
            rule(keywords("if|else|repeat|while|function|for|in|next|break|TRUE|FALSE|NULL|Inf|NaN|NA|library|require|source"), KEYWORD)
            rule("\\b[a-zA-Z.][A-Za-z0-9._]*(?=\\s*\\()", FUNCTION)
        }
    }
}
