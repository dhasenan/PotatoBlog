package potatoblog

import com.google.inject.*
import com.google.inject.matcher.*
import com.google.inject.spi.*
import com.google.common.eventbus.EventBus
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Shell

class MatchAll() : Matcher<Any> {
  override fun matches(o: Any): Boolean {
    return true
  }
}

class BusListener(val bus: EventBus) : ProvisionListener {
  override fun <T: Any> onProvision(it: ProvisionListener.ProvisionInvocation<T>) {
    val s = it.provision()
    bus.register(s)
  }
}

class PotatoModule : AbstractModule() {
  override protected fun configure() {
    val bus = EventBus("potato")
    bind(EventBus::class.java).toInstance(bus)
    val display = Display()
    bind(Display::class.java).toInstance(display)
    bind(Shell::class.java).toInstance(Shell(display))
    bindListener(
      MatchAll(),
      BusListener(bus))
  }
}

@Singleton
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
  val server = injector.getInstance(BlogServer::class.java)
  val serverThread = Thread({
    server.startServer()
  })
  serverThread.setDaemon(true)
  serverThread.start()
  val gui = injector.getInstance(MainGui::class.java)
  gui.run(args)
  server.stopServer()
}
