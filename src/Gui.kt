package potatoblog

import com.google.common.eventbus.Subscribe
import org.eclipse.swt.*
import org.eclipse.swt.browser.*
import org.eclipse.swt.custom.*
import org.eclipse.swt.events.*
import org.eclipse.swt.layout.*
import org.eclipse.swt.widgets.*
import com.google.common.eventbus.EventBus
import jakarta.inject.*

abstract class View<TComposite> where TComposite : Widget {
  lateinit var self: TComposite

  var parent: Widget? = null
  set(value) {
    field = value
    build()
    val s = self
    if (s is Control) {
      s.pack()
    }
  }

  abstract fun build()
}

class NewPostPopup @Inject constructor(val display: Display, val bus: EventBus) {
  // - path
  // - markdown vs HTML
  // - post vs page
  val self = Shell(display, SWT.APPLICATION_MODAL.or(SWT.DIALOG_TRIM))
  val pathLabel = Label(self, 0)
  val pathEntry = Text(self, SWT.SINGLE)
  val formatLabel = Label(self, 0)
  val formatSelect = Combo(self, SWT.DROP_DOWN.or(SWT.READ_ONLY))
  val typeLabel = Label(self, 0)
  val typeSelect = Combo(self, SWT.DROP_DOWN.or(SWT.READ_ONLY))
  val buttonContainer = Composite(self, 0)
  val ok = Button(buttonContainer, SWT.RIGHT)
  val cancel = Button(buttonContainer, SWT.RIGHT)
  init {
    self.layout = GridLayout(2, true)
    val buttonContainerLayout = RowLayout()
    buttonContainerLayout.pack = true
    buttonContainer.layout = buttonContainerLayout

    pathLabel.text = "&Path"
    pathLabel.layoutData = GridData(SWT.LEFT, SWT.FILL, false, false)
    pathEntry.layoutData = GridData(SWT.FILL, SWT.FILL, true, false)
    pathEntry.addModifyListener({
      ok.enabled = (pathEntry.text.length > 0)
    })
    formatLabel.text = "Format"
    formatLabel.layoutData = GridData(SWT.LEFT, SWT.FILL, false, false)
    formatSelect.layoutData = GridData(SWT.FILL, SWT.FILL, true, false)
    formatSelect.setItems("Markdown", "HTML")
    formatSelect.data = listOf(BodyType.MARKDOWN, BodyType.HTML)
    typeLabel.text = "Type"
    typeLabel.layoutData = GridData(SWT.LEFT, SWT.FILL, false, false)
    typeSelect.layoutData = GridData(SWT.FILL, SWT.FILL, true, false)
    typeSelect.setItems("Post", "Page")
    typeSelect.data = listOf(PostType.BLOGPOST, PostType.PAGE)
    buttonContainer.layoutData = GridData(SWT.FILL, SWT.FILL, true, false, 2, 1)
    ok.text = "&Ok"
    ok.addListener(SWT.Selection, {evt -> okClicked() })
    cancel.text = "&Cancel"
    cancel.addListener(SWT.Selection, {evt -> self.close() })
  }

  fun okClicked() {
    val post = Post()
    post.path = pathEntry.text
    val btd = formatSelect.data as List<BodyType>
    post.bodyType = btd[formatSelect.selectionIndex]
    val ptd = typeSelect.data as List<PostType>
    post.type = ptd[typeSelect.selectionIndex]
    bus.post(FileAdded(post))
    bus.post(FileOpened(post))
    self.close()
  }

  fun show() {
    typeSelect.select(0)
    pathEntry.text = ""
    ok.enabled = false
    self.open()
  }
}

@Singleton
class ErrorDialog @Inject constructor(val display: Display) {
  val self = Shell(display, SWT.APPLICATION_MODAL.or(SWT.DIALOG_TRIM))
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

@Singleton
class Swapper(val parent: Composite, val widgets: List<Composite>) {
  val self = Composite(parent, SWT.NONE)
  init {
    self.layout = GridLayout(1, false)
    for (widget in widgets) {
      val data = GridData()
      data.exclude = true
      widget.layoutData = data
      widget.visible = false
    }
    if (widgets.size > 0) {
      show(widgets[0])
    }
  }

  fun show(widget: Composite) {
    if (!widgets.contains(widget)) {
      throw IllegalArgumentException(
        "attempted to show widget $widget that isn't part of this control")
    }
    widget.visible = true
    (widget.layoutData as GridData).exclude = false
  }
}

fun ctrl(c: Char): Int {
  return SWT.MOD1 or c.code
}

@Singleton
class FileMenuView @Inject constructor(
  val controller: FileMenuController,
  val shell: Shell,
  val injector: com.google.inject.Injector
) : View<Menu>() {
  override fun build() {
    controller.view = this
    val parentMenu = parent as Menu
    val fileItem = MenuItem(parentMenu, SWT.CASCADE)
    val fileMenu = Menu(parentMenu)
    self = fileMenu
    fileItem.text = "&File"
    fileItem.menu = fileMenu

    val newItem = MenuItem(fileMenu, SWT.PUSH)
    newItem.text = "New &Blog"
    newItem.addListener(SWT.Selection, {e -> controller.newBlog()})

    val newPostItem = MenuItem(fileMenu, SWT.PUSH)
    newPostItem.text = "&New Post/Page"
    newPostItem.addListener(SWT.Selection, {e ->
      val newPost = injector.getInstance(NewPostPopup::class.java)
      newPost.show()
    })
    newPostItem.accelerator = ctrl('n')

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
    val fod = FileDialog(shell, SWT.SAVE)
    fod.filterExtensions = arrayOf("*.pblog")
    return fod.open()
  }

  fun getOpenPath(): String? {
    val fod = FileDialog(shell, SWT.OPEN)
    fod.filterExtensions = arrayOf("*.pblog")
    return fod.open()
  }
}

@Singleton
class FileMenuController @Inject constructor(
  val ctx: Context,
  val errors: ErrorDialog,
  val persist: Persist,
  val newPostPopup: NewPostPopup
) {
  lateinit var view: FileMenuView
  fun saveBlog() {
    var path = ctx.blogPath
    val blog = ctx.blog
    if (path == null || blog == null) return
    persist.save(blog, path)
  }

  fun newBlog() {
    // TODO check if there's an open blog already, prompt to save
    val path = view.getSavePath()
    if (path != null) ctx.openBlog(defaultBlog(), path)
  }

  fun openBlog() {
    // TODO check if there's an open blog already, prompt to save
    val path = view.getSavePath()
    if (path == null) return;
    var blog: Blog? = null
    try {
      blog = loadBlog(path)
    } catch (e: BlogLoadException) {
      errors.show(e.message ?: "Unknown error")
      return
    }
    ctx.openBlog(loadBlog(path), path)
  }

  fun newPost() {
    newPostPopup.show()
  }

  fun quit() {
    ctx.eventBus.post(PleaseQuit())
  }
}

@Singleton
class MainMenu @Inject constructor(
  val shell: Shell,
  val files: FileMenuView,
) {
  init {
    val menu = Menu(shell, SWT.BAR)
    shell.menuBar = menu
    files.parent = menu
  }
}

class PostEditView(val ctx: Context, val parent: Composite) {
  val view = Composite(parent, SWT.NONE)
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

class BlogTreeView @Inject constructor(val bus: EventBus): View<Tree>() {
  val pathToItem = mutableMapOf<String, TreeItem>()
  var blog: Blog? = null

  override fun build() {
    val c = Composite(parent as Composite, 0)
    c.layout = GridLayout(1, false)
    val header = Label(c, SWT.HORIZONTAL)
    header.text = "Blog!"
    header.pack()
    self = Tree(c, SWT.MULTI.or(SWT.BORDER))
    self.layoutData = GridData(GridData.FILL_HORIZONTAL.or(GridData.FILL_VERTICAL))
    self.clearAll(true)
  }

  fun item(path: String): TreeItem {
    if (pathToItem.containsKey(path)) {
      return pathToItem[path]!!
    }
    val p = pathParent(path)
    if (p == path) {
      // root level item
      val ti = TreeItem(self, 0)
      ti.expanded = true
      ti.text = basename(p)
      pathToItem[p] = ti
      return ti
    }
    val parent = item(p)
    val ti = TreeItem(parent, 0)
    ti.expanded = true
    ti.text = basename(path)
    pathToItem[path] = ti
    ti.addListener(SWT.Activate, { evt -> 
      val data = ti.data
      if (data is BlogFile) {
        bus.post(FileOpened(data))
      }
    })
    return ti
  }

  @Subscribe
  fun changedBlog(e: ChangedBlog) {
    blog = e.blog
    self.clearAll(true)
    // TODO sorting
    for (post in e.blog.posts) {
      item(post.path).data = post
    }
    for (staticFile in e.blog.staticFiles) {
      item(staticFile.path).data = staticFile
    }
  }

  @Subscribe
  fun fileAdded(e: FileAdded) {
    item(e.file.path).data = e.file
  }

  fun remove(it: TreeItem) {
    var parent = it.parentItem
    if (parent != null) {
      parent.clear(parent.indexOf(it), true)
      if (parent.itemCount == 0) {
        remove(parent)
      }
    } else {
      self.clear(self.indexOf(it), true)
    }
  }

  @Subscribe
  fun fileRemoved(e: FileRemoved) {
    if (e.file.path in pathToItem) {
      val it = pathToItem[e.file.path]
    }
  }
  @Subscribe
  fun fileRenamed(e: FileRenamed) {
    // TODO
  }
}

class BlogTreeController {
  lateinit var view: BlogTreeView
}

class MainGui @Inject constructor(
    val ctx: Context,
    val shell: Shell,
    val display: Display,
    val mainMenu: MainMenu,
    val fileTree: BlogTreeView,
  ) : ShellAdapter() {
  init {
    shell.text = "PotatoBlog"
    val grid = GridLayout(1, false)
    shell.layout = grid

    //buildMenu(menubar, shell)


    val sash = SashForm(shell, SWT.HORIZONTAL)
    sash.layoutData = GridData(SWT.FILL, SWT.FILL, true, true)
    fileTree.parent = sash
    //val fileTree = Tree(sash, SWT.SINGLE)
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
