package potatoblog

import org.eclipse.swt.*
import org.eclipse.swt.browser.*
import org.eclipse.swt.custom.*
import org.eclipse.swt.layout.*
import org.eclipse.swt.widgets.*
import com.google.inject.*
import com.google.common.eventbus.EventBus

class NewBlogWindow(val ctx: Context) {

}

fun loadBlog(path: String) {}

fun showNewBlogWindow() {}

fun saveBlog() {}

fun buildMenu(menubar: Menu, shell: Shell) {
  val fileItem = MenuItem(menubar, SWT.CASCADE)
  val fileMenu = Menu(menubar)
  fileItem.text = "&File"
  fileItem.menu = fileMenu

  val newItem = MenuItem(fileMenu, SWT.PUSH)
  newItem.text = "&New"
  newItem.addListener(SWT.Selection, {e -> showNewBlogWindow()})

  val openItem = MenuItem(fileMenu, SWT.PUSH)
  openItem.text = "&Open"
  openItem.addListener(SWT.Selection, {e ->
    val fod = FileDialog(shell, SWT.OPEN)
    fod.filterExtensions = arrayOf("*.pblog")
    val path = fod.open()
    if (path != null) {
      loadBlog(path)
    }
  })

  val saveItem = MenuItem(fileMenu, SWT.PUSH)
  saveItem.text = "&Save"
  saveItem.addListener(SWT.Selection, {e -> saveBlog()})
}

class MainGui {
  val display = Display()
  val shell = Shell(display)
  val eventBus: EventBus

  @Inject
  constructor(eventBus: EventBus) {
    this.eventBus = eventBus
    shell.text = "PotatoBlog"
    val grid = GridLayout(1, false)
    shell.layout = grid
    val menubar = Menu(shell, SWT.BAR)
    shell.menuBar = menubar

    buildMenu(menubar, shell)


    val sash = SashForm(shell, SWT.HORIZONTAL)
    sash.layoutData = GridData(SWT.FILL, SWT.FILL, true, true)
    val fileTree = Tree(sash, SWT.SINGLE)
    val editor = StyledText(sash, SWT.WRAP or SWT.FILL)
    val preview = Browser(sash, SWT.FILL)
    preview.url = "https://example.org"
    sash.setWeights(1, 4, 4)

    shell.open()
  }

  fun run() {
    while (!shell.isDisposed()) {
      if (!display.readAndDispatch()) {
        display.sleep()
      }
    }
    eventBus.post(ShuttingDown())
    display.dispose()
  }
}
