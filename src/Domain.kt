package potatoblog

import java.time.*
import java.io.*
import java.net.URLConnection
import org.apache.commons.io.IOUtils
import com.fasterxml.jackson.annotation.JsonIgnore

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

const val DEFAULT_THEME_NAME = "defaultTheme"
fun defaultTheme(): Theme {
  val cl = Blog::class.java.classLoader
  val lister = BufferedReader(InputStreamReader(cl.getResourceAsStream(DEFAULT_THEME_NAME)))
  val theme = Theme()
  theme.name = "Default Theme"
  while (true) {
    val resourceName = lister.readLine()
    if (resourceName == null) break
    val sf = StaticFile()
    sf.data = IOUtils.toByteArray(cl.getResourceAsStream(resourceName))
    sf.path = resourceName.substring(DEFAULT_THEME_NAME.length)
    theme.files.add(sf)
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
