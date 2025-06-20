package potatoblog

import java.time.*
import java.io.*
import java.net.URLConnection
import org.apache.commons.io.IOUtils

class BlogLoadException (msg: String): Exception(msg)

fun defaultBlog(): Blog {
  val blog = Blog()
  val firstPost = Post()
  firstPost.status = PostStatus.PUBLISHED
  blog.posts.add(firstPost)
  val aboutMe = Post()
  aboutMe.type = PostType.PAGE
  aboutMe.url = "/about.html"
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
    sf.mimeType = URLConnection.guessContentTypeFromName(resourceName)
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
  var theme = defaultTheme()
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

class Post {
  var type = PostType.BLOGPOST
  var status = PostStatus.DRAFT
  var title = "A Very Good Post"
  var url = ""
  var publishDate = Clock.systemDefaultZone().instant()
  var bodyType = BodyType.MARKDOWN
  var body = "This is my post, and I am proud of it"
}

class StaticFile {
  var path = "static/newfile"
  var data = ByteArray(0)
  var mimeType = "application/octet-stream"
  var publishDate = Clock.systemDefaultZone().instant()

  fun asString(): String? {
    return null
  }
}

class Theme {
  var name = "Custom theme"
  var files: MutableList<StaticFile> = ArrayList()
}
