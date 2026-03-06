package potatoblog

import com.google.inject.*
import com.google.common.eventbus.EventBus
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Shell

class PotatoModule : AbstractModule() {
  override protected fun configure() {
    val bus = EventBus("potato")
    bind(EventBus::class.java).toInstance(bus)
    val display = Display()
    bind(Display::class.java).toInstance(display)
    bind(Shell::class.java).toInstance(Shell(display))
  }
}

class Context @Inject constructor(val eventBus: EventBus) {
  var blog: Blog? = null
    private set
  var blogPath: String? = null
    private set
  var unsavedData = false

  fun openBlog(b: Blog, path: String) {
    this.blog = b
    this.blogPath = path
    eventBus.post(ChangedBlog(b))
  }
}

fun main(args: Array<String>) {
  val injector = Guice.createInjector(PotatoModule())
  val gui = injector.getInstance(MainGui::class.java)
  gui.run()
}
