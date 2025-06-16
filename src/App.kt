package potatoblog

import com.google.inject.*
import com.google.common.eventbus.EventBus

class PotatoModule : AbstractModule() {
  override protected fun configure() {
    val bus = EventBus("potato")
    bind(EventBus::class.java).toInstance(bus)
  }
}

class Context {
  var blog: Blog? = null
}


fun main() {
  val injector = Guice.createInjector(PotatoModule())
  val gui = injector.getInstance(MainGui::class.java)
  gui.run()
}
