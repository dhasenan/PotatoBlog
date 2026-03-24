package potatoblog

import com.github.mustachejava.*
import com.github.mustachejava.codes.*
import com.github.mustachejava.reflect.*
import com.github.mustachejava.resolver.*

import jakarta.inject.Singleton
import java.io.*
import java.io.FileOutputStream
import java.lang.reflect.*
import java.nio.charset.StandardCharsets
import java.nio.file.*
import java.util.zip.*

import com.github.mustachejava.util.HtmlEscaper.escape

val unsafeMethods = setOf(
  "getClass",
  "clone",
  "toString",
  "notify",
  "notifyAll",
  "finalize",
  "wait")

class SafeObjectHandler : SimpleObjectHandler() {
  override fun checkMethod(member: Method) {
    if (unsafeMethods.contains(member.name))
      throw MustacheException("${member.name} is inaccessible for security reasons")
    if ((member.modifiers and Modifier.PUBLIC) != Modifier.PUBLIC)
      throw MustacheException("${member.name} is not public")
  }

  override fun checkField(member: Field) {
    if ((member.modifiers and Modifier.PUBLIC) != Modifier.PUBLIC)
      throw MustacheException("${member.name} is not public")
  }
}

class BlogResolver(val blog: Blog) : DefaultResolver() {
  override fun getReader(name: String): Reader? {
    for (file in blog.theme.files) {
      if (file.path.endsWith("mustache")) {
        if (bareFileName(file.path) == name) {
          return StringReader(String(file.data, StandardCharsets.UTF_8))
        }
      }
    }
    return null
  }
}

class PotatoMustacheFactory(val blog: Blog) : DefaultMustacheFactory(BlogResolver(blog)) {
  init {
    objectHandler = SafeObjectHandler()
    mc.setAllowChangingDelimeters(false)
  }

  override fun createMustacheVisitor(): MustacheVisitor {
    return NoPragmaVisitor(this)
  }
}

class NoPragmaVisitor(factory: DefaultMustacheFactory) : DefaultMustacheVisitor(factory) {
  override fun pragma(tc: TemplateContext, pragma: String, args: String) {
    throw MustacheException("Disallowed: pragmas in templates")
  }
}

class RenderContext (
  val blog: Blog,
  val post: Post,
  val mainContent: String = "",
)

class Renderer(val blog: Blog) {
  val factory = PotatoMustacheFactory(blog)
  val page = factory.compile("page")
  val post = factory.compile("post")
  val default = factory.compile("default")

  fun renderSingle(post: Post, overrideContents: String? = null): String {
    // 1. Render post.html or page.html as appropriate
    // 2. Render the results inside default.html
    val baseTemplate = when (post.type) {
      PostType.PAGE -> this.page 
      PostType.BLOGPOST -> this.post 
    }
    var sw = StringWriter()
    val tmp = post.body
    if (overrideContents != null) {
      post.body = overrideContents
    }
    try {
      baseTemplate.execute(sw, RenderContext(blog, post))
    } finally {
      post.body = tmp
    }

    val rc = RenderContext(blog, post, sw.toString())
    sw = StringWriter()
    default.execute(sw, rc)
    return sw.toString()
  }

  fun renderVisitor(visitor: RenderVisitor) {
    visitor.open(blog)
    for (post in blog.posts) {
      visitor.directory(pathParent(post.path))
      val s = renderSingle(post)
      val data = StandardCharsets.UTF_8.encode(s)
      val array = ByteArray(data.remaining())
      data.get(array)
      visitor.file(post.path, array)
    }
    for (sf in blog.staticFiles) {
      visitor.directory(pathParent(sf.path))
      visitor.file(sf.path, sf.data)
    }
    for (sf in blog.theme.files) {
      if (sf.shouldCopy) {
        visitor.directory(pathParent(sf.path))
        visitor.file(sf.path, sf.data)
      }
    }
    visitor.close()
  }
}

interface RenderVisitor {
  fun open(blog: Blog)
  fun directory(dir: String)
  fun file(path: String, data: ByteArray)
  fun close()
}

class ZipVisitor(val zipPath: String): RenderVisitor {
  val zip = ZipOutputStream(FileOutputStream(zipPath))
  override fun open(blog: Blog) {}
  override fun directory(dir: String) {}

  override fun file(path: String, data: ByteArray) {
    zip.putNextEntry(ZipEntry(path))
    zip.write(data, 0, data.size)
  }

  override fun close() {
    zip.flush()
    zip.close()
  }
}

class FilesystemVisitor(val basePath: String): RenderVisitor {
  override fun open(blog: Blog) {}

  override fun directory(dir: String) {
    val path = Paths.get(basePath, dir)
    Files.createDirectories(path)
  }

  override fun file(filePath: String, data: ByteArray) {
    println("writing ${filePath} with ${data.size} bytes")
    val path = Paths.get(basePath, filePath)
    Files.write(path, data)
  }

  override fun close() {}
}
