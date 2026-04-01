// Embedded jetty server for previewing stuff
package potatoblog

import org.eclipse.jetty.server.*
import org.eclipse.jetty.http.*
import org.eclipse.jetty.io.*
import org.eclipse.jetty.util.Callback
import java.nio.ByteBuffer
import jakarta.inject.*

val SERVER_PORT = 20145

val DEFAULT_HTML = """
<html>
  <body>
    Select a post to see a preview!
  </body>
</html>
"""

fun errorHTML(path: String, err: Exception): String {
  val stackTrace = java.io.StringWriter()
  err.printStackTrace(java.io.PrintWriter(stackTrace, true))
  return """
<html>
  <body>
    Error while retrieving ${path}<br>
    ${err.message}
    <pre>${stackTrace.toString()}</pre>
  </body>
</html>
"""
}

@Singleton
class BlogServer @Inject constructor(val ctx: Context): Handler.Abstract() {
  private val server = Server(SERVER_PORT)
  init {
    server.handler = this
  }

  fun startServer() {
    server.start()
  }

  fun stopServer() {
    server.stop()
  }

  fun htmlHeader(resp: Response) {
    resp.headers.put(HttpHeader.CONTENT_TYPE, "text/html; charset=utf-8")
  }

  private fun samePath(a: String, b: String): Boolean {
    var aa: CharSequence = a
    var bb: CharSequence = b
    if (aa.startsWith("/")) {
      aa = aa.substring(1)
    }
    if (bb.startsWith("/")) {
      bb = bb.substring(1)
    }
    return aa == bb
  }

  override fun handle(req: Request, resp: Response, cb: Callback): Boolean {
    val blog = ctx.blog
    if (blog == null) {
      htmlHeader(resp)
      Content.Sink.write(resp, true, DEFAULT_HTML, cb)
      return true
    }
    val path = req.httpURI.path
    // find something for this path
    // try posts
    for (post in blog.posts) {
      if (samePath(post.path, path)) {
        val text = Renderer(blog).renderSingle(post, post.unsavedBody)
        htmlHeader(resp)
        Content.Sink.write(resp, true, text, cb)
        return true
      }
    }

    // try static files
    for (file in blog.staticFiles) {
      if (samePath(file.path, path)) {
        resp.headers.put(HttpHeader.CONTENT_TYPE, file.mimeType)
        Content.Sink.write(resp, true, ByteBuffer.wrap(file.data))
        cb.succeeded()
        return true
      }
    }

    // try static files
    for (file in blog.theme.files) {
      if (samePath(file.path, path)) {
        resp.headers.put(HttpHeader.CONTENT_TYPE, file.mimeType)
        Content.Sink.write(resp, true, ByteBuffer.wrap(file.data))
        cb.succeeded()
        return true
      }
    }

    // not found
    htmlHeader(resp)
    Content.Sink.write(resp, true, DEFAULT_HTML, cb)
    return true
  }
}

