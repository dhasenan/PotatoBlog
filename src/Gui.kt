package potatoblog

import com.google.common.eventbus.Subscribe
import org.eclipse.swt.*
import org.eclipse.swt.browser.*
import org.eclipse.swt.custom.*
import org.eclipse.swt.events.*
import org.eclipse.swt.layout.*
import org.eclipse.swt.widgets.*
import com.google.common.eventbus.EventBus
import jakarta.inject.Inject

abstract class View<TComposite> where TComposite : Widget {
  lateinit var self: TComposite

  var parent: Composite? = null
  set(value) {
    field = value
    build()
  }

  abstract fun build()
}

class ErrorDialog @Inject constructor(val display: Display) {
  val self = Shell(display, SWT.APPLICATION_MODAL)
  val label = Label(self, SWT.NONE)
  val okButton = Button(self, SWT.PUSH)
  init {
    self.layout = RowLayout()
    label.image = display.getSystemImage(SWT.ICON_ERROR)
    okButton.text = "&OK"
    okButton.addListener(SWT.Selection, {e -> self.close()})
  }

  fun show(message: String) {
    label.text = message
    self.open()
  }
}

fun ctrl(c: Char): Int {
  return SWT.MOD1 or (c as Int)
}

class FileMenuView @Inject constructor(val controller: FileMenuController) : View<Menu>() {
  override fun build() {
    val parentMenu = parent as Menu
    val fileItem = MenuItem(parentMenu, SWT.CASCADE)
    val fileMenu = Menu(parentMenu)
    fileItem.text = "&File"
    fileItem.menu = fileMenu

    val newItem = MenuItem(fileMenu, SWT.PUSH)
    newItem.text = "&New"
    newItem.addListener(SWT.Selection, {e -> controller.newBlog()})
    newItem.accelerator = ctrl('n')

    val openItem = MenuItem(fileMenu, SWT.PUSH)
    openItem.text = "&Open"
    openItem.addListener(SWT.Selection, {e -> controller.openBlog()})
    newItem.accelerator = ctrl('o')

    val saveItem = MenuItem(fileMenu, SWT.PUSH)
    saveItem.text = "&Save"
    saveItem.addListener(SWT.Selection, {e -> controller.saveBlog()})
    newItem.accelerator = ctrl('s')

    val quitItem = MenuItem(fileMenu, SWT.PUSH)
    quitItem.text = "&Quit"
    quitItem.addListener(SWT.Selection, {e -> controller.quit()})
    newItem.accelerator = ctrl('q')
  }

  fun getSavePath(): String? {
    val fod = FileDialog(parent as Shell, SWT.SAVE)
    fod.filterExtensions = arrayOf("*.pblog")
    return fod.open()
  }

  fun getOpenPath(): String? {
    val fod = FileDialog(parent as Shell, SWT.OPEN)
    fod.filterExtensions = arrayOf("*.pblog")
    return fod.open()
  }

  fun showErrorMessage(msg: String) {

  }
}

class FileMenuController @Inject constructor(val ctx: Context) {
  @Inject lateinit var view: FileMenuView
  fun saveBlog() {
    var path = ctx.blogPath
    val blog = ctx.blog
    if (path == null || blog == null) return
    blog.save(path)
  }

  fun newBlog() {
    // TODO check if there's an open blog already, prompt to save
    val path = view.getSavePath()
    if (path != null) ctx.openBlog(Blog(), path)
  }

  fun openBlog() {
    // TODO check if there's an open blog already, prompt to save
    val path = view.getSavePath()
    if (path == null) return;
    var blog: Blog? = null
    try {
      blog = loadBlog(path)
    } catch (e: BlogLoadException) {
      view.showErrorMessage(e.message ?: "Unknown error")
      return
    }
    ctx.openBlog(loadBlog(path), path)
  }

  fun quit() {
    ctx.eventBus.post(PleaseQuit())
  }
}

class MainMenu(val shell: Shell, val ctx: Context) {
  init {
  }
}

fun buildMenu(menubar: Menu, shell: Shell) {
}

class PostEditView(val ctx: Context, val parent: Composite) {
  val view = Composite(parent, SWT.NONE)
  //val sourcePane =
  init {

  }
}

class EditView(val ctx: Context, val parent: Composite) {
  val view = Composite(parent, SWT.NONE)
  val postEdit = PostEditView(ctx, view)
  init {
    view.setLayout(GridLayout(1, true))
    postEdit.view.layoutData = GridData(SWT.FILL, SWT.FILL, true, true)
  }
}

class MainGui @Inject constructor(val ctx: Context, val display: Display) : ShellAdapter() {
  //val display = Display()
  val shell = Shell(display)

  init {
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
  }

  fun run() {
    shell.open()

    while (!shell.isDisposed()) {
      if (!display.readAndDispatch()) {
        display.sleep()
      }
    }
    ctx.eventBus.post(ShuttingDown())
    display.dispose()
  }

  @Subscribe
  fun pleaseQuit(p: PleaseQuit) {
    shell.close()
  }

  override fun shellClosed(e: ShellEvent) {
    // TODO "do you want to save before you quit?" dialog
  }
}
