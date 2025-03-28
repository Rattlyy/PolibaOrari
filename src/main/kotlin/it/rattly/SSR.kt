package it.rattly

import klite.*
import org.graalvm.polyglot.Context
import org.intellij.lang.annotations.Language
import java.io.File
import java.io.OutputStream
import java.nio.file.Path
import kotlin.system.measureTimeMillis

fun Server.ssr(aa: String) {
    val indexJSBundle =
        File(if (Config.isDev) "src/main/javascript/dist/client/assets" else "/web/client/assets").list()
            .first { it.startsWith("index") && it.endsWith("js") }

    val ctx: Context = Context.newBuilder()
        .option("js.esm-eval-returns-exports", "true")
        .option("js.commonjs-require", "true")
        .option("js.text-encoding", "true")
        .option(
            "js.commonjs-require-cwd",
            Path.of("${if (Config.isDev) "src/main/resources/packages/" else "/web/"}node_modules").toAbsolutePath()
                .toString()
        )
        .option("js.unhandled-rejections", "throw")
        .allowAllAccess(true).build().apply {
            eval("js", polyfills)
            eval(
                "js",
                File(
                    if (Config.isDev) "./src/main/javascript/dist/server/entry-server.js"
                    else "/web/server/entry-server.js"
                ).readText().replace(Regex("""export\s*\{\s*(\w+,\s*)*(\w+)\s*};"""), "")
            )
        }

    context("/") {
        get(".*") {
            val helper = StreamHelper(this, aa)
            try {
                val time = measureTimeMillis {
                    //TODO: Monkey patch; if this needs to run in a serious context (not a 4€ VPS from Hetzner) then
                    //TODO: create a context pool, instantiating N contextes for N threads and using them in a round-robin fashion
                    synchronized(ctx) {
                        ctx.eval("js", "ssr").execute(
                            this@get.path,
                            "/assets/$indexJSBundle",
                            helper,
                        )
                    }
                }

                if (Config.isDev) {
                    println("Time: $time ms")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                (helper.stream ?: startResponse(StatusCode.InternalServerError)).write(
                    @Language("HTML") """
                    <html>
                        <head>
                            <title>500 Internal Server Error</title>
                        </head>
                        <body>
                            <h1>Exception</h1>
                            <code>${
                        e.stackTraceToString().replace("<", "").replace(">", "").replace("\n", "<br />")
                    }</code>
                        </body>
                    </html>
                """.trimIndent().toByteArray()
                )
            }
        }

        MimeTypes.byExtension.put("webmanifest", "application/manifest+json") //TODO: klite merges pr
        assets(
            "/assets",
            AssetsHandler(Path.of(if (Config.isDev) "./src/main/javascript/dist/client" else "/web/client"))
        )
    }
}

@Suppress("unused")
class StreamHelper(val req: HttpExchange, val aa: String) {
    // Hack so GraalJS doesn't just make stream null
    var stream: OutputStream? = null
    fun stream() = stream
    fun aa() = aa

    fun write(int: Int) {
        stream?.write(int)
    }

    fun write(intArray: IntArray) {
        stream?.write(intArray.map { it.toByte() }.toByteArray())
    }

    fun startResponse(statusCode: String) {
        stream = req.startResponse(if (statusCode == "success") StatusCode.OK else StatusCode.InternalServerError)
    }

    fun header(key: String, value: String) {
        req.header(key, value)
    }
}

@Suppress("JSUnresolvedReference", "NodeCoreCodingAssistance", "JSDuplicatedDeclaration")
@Language("JavaScript")
val polyfills =
    """
        var process = {
            env: {
                NODE_DEBUG: false
            }
        }
        
        var ReadableStream = require("web-streams-polyfill").ReadableStream;
        var WritableStream = require("web-streams-polyfill").WritableStream;
        var Buffer = require('buffer/').Buffer
        var URL = require('url').Url
        var window = this;
        var global = {};
        var SecureRandom = Java.type('java.security.SecureRandom');
        var crypto = {
            getRandomValues: (buf) => {
                var bytes = SecureRandom.getSeed(buf.length);
                buf.set(bytes);
            }
        }
        
        let encoder = new TextEncoder();
        function btoa(str) {
            return Java.type('java.util.Base64').getEncoder().encodeToString(encoder.encode(str));
        }
        function fetch(resource, options) {}
        function fetch(resource) {}
        function setTimeout(callback,delay) {}
        function queueMicrotask(callback) {}

        """.trimIndent()