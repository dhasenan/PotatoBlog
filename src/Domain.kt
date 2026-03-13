package potatoblog

import java.time.*
import java.io.*
import java.net.URLConnection
import org.apache.commons.io.IOUtils
import com.fasterxml.jackson.annotation.JsonIgnore
import org.reflections.*
import org.reflections.scanners.Scanners.*

class BlogLoadException (msg: String): Exception(msg)

fun defaultBlog(): Blog {
  val blog = Blog()
  val firstPost = Post()
  firstPost.status = PostStatus.PUBLISHED
  firstPost.path = "/posts/first.html"
  blog.posts.add(firstPost)
  val aboutMe = Post()
  aboutMe.type = PostType.PAGE
  aboutMe.path = "/about.html"
  aboutMe.body = "I'm a person with a blog!"
  aboutMe.status = PostStatus.PUBLISHED
  blog.posts.add(aboutMe)
  return blog
}

fun defaultTheme(): Theme {
  val prefix = "potatoblog/defaultTheme/"
  val theme = Theme()
  theme.name = "Default Theme"
  // This should be built into Java. Alas.
  val reflections = org.reflections.Reflections(org.reflections.util.ConfigurationBuilder()
    .forPackage("potatoblog")
    .addScanners(org.reflections.scanners.Scanners.Resources)
    .addClassLoaders(Blog::class.java.classLoader))
  // These filenames are full resource paths
  val filenames = reflections.getAll(org.reflections.scanners.Scanners.Resources)
  for (filename in filenames) {
    if (filename.indexOf(prefix) < 0) {
      continue
    }
    // The files are stored at potatoblog/defaultTheme/whatever
    // I need to strip off the first two path components
    val name = filename.substring(prefix.length, filename.length)
    val file = StaticFile()
    file.path = name

    // Guava needs the resource path relative to the directory containing the class I give it.
    // This is, needless to say, annoying.
    val resourceName = "defaultTheme/" + name
    val uri = com.google.common.io.Resources.getResource(Blog::class.java, resourceName)
    file.data = com.google.common.io.Resources.toByteArray(uri)
    theme.files.add(file)
  }
  return theme
}

fun loadBlog(path: String): Blog {
  throw BlogLoadException("not yet implemented")
}

class Blog {
  var name = "My Blog"
  var baseURL = "https://my-blog.invalid"
  var author = "My Own Self"
  var staticFiles: List<StaticFile> = ArrayList()
  @JsonIgnore var theme = defaultTheme()
  val posts: MutableList<Post> = ArrayList()

  fun resource(name: String): StaticFile? {
    return null;
  }

  fun save(path: String) {
    // TODO

  }
}

enum class PostType {
  PAGE,
  BLOGPOST,
}

enum class BodyType {
  MARKDOWN,
  HTML,
}

enum class PostStatus {
  PUBLISHED,
  DRAFT
}

fun now(): ZonedDateTime = ZonedDateTime.ofInstant(
  Clock.systemDefaultZone().instant(),
  Clock.systemDefaultZone().zone)

interface BlogFile {
  var path: String
  val mimeType: String
}

class Post: BlogFile {
  var type = PostType.BLOGPOST
  var status = PostStatus.DRAFT
  var title = "A Very Good Post"
  override var path = ""
  var publishDate = now()
  var bodyType = BodyType.MARKDOWN
  @JsonIgnore
  var body = "This is my post, and I am proud of it"

  override val mimeType: String
    get() {
      return when (bodyType) {
        BodyType.MARKDOWN -> "text/markdown"
        BodyType.HTML -> "text/html"
      }
    }
}

enum class FileUsage {
  PUBLISH,
  TEMPLATE,
}

class StaticFile: BlogFile {
  var usage = FileUsage.PUBLISH
  override var path = "static/newfile"
  override val mimeType: String
    @JsonIgnore get() {
      return URLConnection.guessContentTypeFromName(path) ?: "application/octet-stream"
    }
  @JsonIgnore
  var data = ByteArray(0)

  fun asString(): String? {
    return null
  }
}

class Theme {
  var name = "Custom theme"
  @JsonIgnore var files: MutableList<StaticFile> = ArrayList()
}

fun pathParent(p: String): String {
  val s = p.lastIndexOf("/")
  if (s <= 0) {
    return p
  }
  return p.subSequence(0, s - 1).toString()
}

fun basename(p: String): String {
  val s = p.lastIndexOf("/")
  if (s < 0) {
    return p
  }
  return p.subSequence(s + 1, p.length).toString()
}
