package potatoblog

import jakarta.inject.*
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.*
import kotlin.reflect.KClass
import org.apache.commons.io.IOUtils
import tools.jackson.dataformat.xml.*

val BLOG_DATA_PATH = "blog.xml"
val THEME_DATA_PATH = "theme.xml"

@Singleton
class Persist {
  val mapper = XmlMapper()

  fun save(blog: Blog, path: String) {
    val zip = ZipOutputStream(FileOutputStream(path))
    writeEntry(zip, BLOG_DATA_PATH, mapper.writeValueAsBytes(blog)) 
    for (f in blog.posts) {
      writeEntry(zip, "blog/" + f.path, StandardCharsets.UTF_8.encode(f.body).array())
    }
    for (f in blog.staticFiles) {
      writeEntry(zip, "blog/" + f.path, f.data)
    }
    writeEntry(zip, THEME_DATA_PATH, mapper.writeValueAsBytes(blog.theme)) 
    for (f in blog.theme.files) {
      writeEntry(zip, "theme/" + f.path, f.data)
    }
    zip.flush()
    zip.close()
  }

  fun load(path: String): Blog {
    val zf = ZipFile(path)
    val blog = readXml<Blog>(zf, BLOG_DATA_PATH, Blog::class)
    blog.theme = readXml(zf, THEME_DATA_PATH, Theme::class)
    for (f in blog.posts) {
      f.body = readString(zf, "blog/" + f)
    }
    for (f in blog.staticFiles) {
      f.data = readBytes(zf, "blog/" + f.path)
    }
    for (f in blog.theme.files) {
      f.data = readBytes(zf, "theme/" + f.path)
    }
    zf.close()
    return blog
  }

  private fun <T : Any> readXml(zf: ZipFile, path: String, clazz: KClass<T>): T {
    val entry = zf.getEntry(path)
    val istream = zf.getInputStream(entry)
    try {
      return mapper.readerFor(clazz.java).readValue(istream)
    } finally {
      istream.close()
    }
  }

  private fun readString(zf: ZipFile, path: String): String {
    val entry = zf.getEntry(path)
    val istream = zf.getInputStream(entry)
    try {
      return IOUtils.toString(istream, StandardCharsets.UTF_8)
    } finally {
      istream.close()
    }
  }

  private fun readBytes(zf: ZipFile, path: String): ByteArray {
    val entry = zf.getEntry(path)
    val istream = zf.getInputStream(entry)
    try {
      return IOUtils.toByteArray(istream)
    } finally {
      istream.close()
    }
  }

  private fun writeEntry(zip: ZipOutputStream, path: String, data: ByteArray) {
    zip.putNextEntry(ZipEntry(path))
    zip.write(data, 0, data.size)
  }
}

