package potatoblog

import java.time.*

class Blog {
  var name = "My Blog"
  var baseURL = "https://my-blog.invalid"
  var author = "My Own Self"

  var staticFiles: List<StaticFile> = ArrayList()

  fun resource(name: String): StaticFile? {
    return null;
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
  var publishDate = Clock.systemDefaultZone().instant()
  var bodyType = BodyType.MARKDOWN
  var body = "This is my post, and I am proud of it"
}

class StaticFile {
  var path = "static/newfile"
  var data = emptyArray<Byte>()
  var mimeType = "application/octet-stream"
  var publishDate = Clock.systemDefaultZone().instant()

  fun asString(): String? {
    return null
  }
}

class Theme {
  var name = "Custom theme"
  var files: List<StaticFile> = ArrayList()
}
