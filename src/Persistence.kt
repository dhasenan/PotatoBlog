package potatoblog

import org.hibernate.*
import org.hibernate.jpa.*

class Persistence {
  fun open(path: String) {
    val cfg = HibernatePersistenceConfiguration("PotatoBlog")
      .showSql(true, true, true)
      .managedClass(Blog::class.java)
      .managedClass(Post::class.java)
      .managedClass(Theme::class.java)
      .managedClass(StaticFile::class.java)
  }
}
