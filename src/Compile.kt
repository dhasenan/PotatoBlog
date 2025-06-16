package potatoblog

import com.github.mustachejava.*
import com.github.mustachejava.codes.*
import com.github.mustachejava.reflect.*
import com.github.mustachejava.resolver.*

import java.io.*
import java.lang.reflect.*

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
  override fun getReader(name: String): Reader {
    val resource = blog.resource(name)
    if (resource == null)
      throw MustacheException("resource $name not found")
    val str = resource.asString()
    if (str == null)
      throw MustacheException("tried to read resource $name as text, but it is ${resource.mimeType}")
    return StringReader(str)
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
