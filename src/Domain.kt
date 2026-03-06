package potatoblog

import java.time.*
import java.io.*
import java.net.URLConnection
import org.apache.commons.io.IOUtils
import jakarta.persistence.*

class BlogLoadException (msg: String): Exception(msg)

fun defaultBlog(): Blog {
  val blog = Blog()
  val firstPost = Post()
  firstPost.status = PostStatus.PUBLISHED
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

@Entity
class Blog {
  @Id @GeneratedValue var id: Int = 0
  @Column var name = "My Blog"
  @Column var baseURL = "https://my-blog.invalid"
  @Column var author = "My Own Self"
  @Column var staticFiles: List<StaticFile> = ArrayList()
  @Column var theme = defaultTheme()
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

@Entity
class Post: BlogFile {
  @Id @GeneratedValue var id: Int = 0

  @Column var type = PostType.BLOGPOST
  @Column var status = PostStatus.DRAFT
  @Column var title = "A Very Good Post"
  @Column override var path = ""
  @Column var publishDate = now()
  @Column var bodyType = BodyType.MARKDOWN
  @Column var body = "This is my post, and I am proud of it"

  @OneToMany var theme: Theme? = null
  @OneToMany var blog: Blog? = null

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

@Entity
class StaticFile: BlogFile {
  @Id @GeneratedValue var id: Int = 0
  @Column var usage = FileUsage.PUBLISH
  @Column override var path = "static/newfile"
  @Column var data = ByteArray(0)
  @Column override var mimeType = "application/octet-stream"
  @Column var publishDate = now()

  fun asString(): String? {
    return null
  }
}

@Entity
class Theme {
  @Id @GeneratedValue var id: Int = 0
  var name = "Custom theme"
  var files: MutableList<StaticFile> = ArrayList()
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
