package potatoblog

import com.google.common.eventbus.Subscribe
import org.eclipse.swt.*
import org.eclipse.swt.browser.*
import org.eclipse.swt.custom.*
import org.eclipse.swt.events.*
import org.eclipse.swt.graphics.*
import org.eclipse.swt.layout.*
import org.eclipse.swt.widgets.*
import com.google.common.eventbus.EventBus
import jakarta.inject.*
import java.util.Timer
import java.nio.file.*
import java.util.stream.Stream

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

class NewPostPopup @Inject constructor(
  val display: Display,
  val bus: EventBus,
  val ctx: Context
) {
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
    ctx.blog!!.posts.add(post)
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

    val renderItem = MenuItem(fileMenu, SWT.PUSH)
    renderItem.text = "&Render"
    renderItem.addListener(SWT.Selection, {e -> controller.renderBlog()})
    newItem.accelerator = ctrl('s')

    val saveItem = MenuItem(fileMenu, SWT.PUSH)
    saveItem.text = "&Save"
    saveItem.addListener(SWT.Selection, {e -> controller.saveBlog()})
    newItem.accelerator = ctrl('s')

    val quitItem = MenuItem(fileMenu, SWT.PUSH)
    quitItem.text = "&Quit"
    quitItem.addListener(SWT.Selection, {e -> controller.quit()})
    newItem.accelerator = ctrl('q')
  }

  fun getRenderPath(): String? {
    val fod = DirectoryDialog(shell, SWT.OPEN)
    val path = fod.open()
    if (path == null) return null
    if (Files.list(Paths.get(path)).findAny().isPresent()) {
      val warning = Shell(shell, SWT.DIALOG_TRIM.or(SWT.APPLICATION_MODAL))
      warning.layout = GridLayout(3, false)
      Label(warning, SWT.NONE).text = "This directory isn't empty. Delete everything in it first?"
    }
    return path
  }

  fun getSavePath(): String? {
    val fod = FileDialog(shell, SWT.SAVE)
    fod.setFilterExtensions(arrayOf("*.pblog"))
    return fod.open()
  }

  fun getOpenPath(): String? {
    val fod = FileDialog(shell, SWT.OPEN)
    fod.setFilterExtensions(arrayOf("*.pblog"))
    return fod.open()
  }
}

@Singleton
class FileMenuController @Inject constructor(
  val ctx: Context,
  val errors: ErrorDialog,
  val persist: Persist,
  val newPostPopup: NewPostPopup,
  val bus: EventBus,
) {
  lateinit var view: FileMenuView
  fun saveBlog() {
    var path = ctx.blogPath
    val blog = ctx.blog
    if (path == null || blog == null) return
    bus.post(BeforeSave())
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
      blog = persist.load(path)
    } catch (e: BlogLoadException) {
      errors.show(e.message ?: "Unknown error")
      return
    }
    ctx.openBlog(blog, path)
  }

  fun renderBlog() {
    val path = view.getRenderPath()
    val blog = ctx.blog
    if (path == null || blog == null) return
    Renderer(blog).renderVisitor(FilesystemVisitor(path))
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

class PostInfoView @Inject constructor(
  val ctx: Context,
  val display: Display,
): View<Composite>() {
  var post: Post? = null
  lateinit var title: Text
  lateinit var path: Text
  lateinit var postType: Combo
  lateinit var publicationStatus: Button
  lateinit var publishDate: DateTime
  lateinit var publishTime: DateTime
  lateinit var expander: ExpandItem
  lateinit var expandBar: ExpandBar
  val listeners = mutableListOf<ModifyListener>()

  override fun build() {
    expandBar = ExpandBar(parent as Composite, SWT.V_SCROLL.or(SWT.FILL))
    expandBar.layout = FillLayout()
    expandBar.layoutData = GridData(SWT.FILL, SWT.TOP, true, false)
    self = Composite(expandBar, SWT.FILL)
    self.layout = GridLayout(2, false)

    fun label(text: String) {
      val label = Label(self, 0)
      label.text = text
      label.layoutData = GridData(SWT.LEFT, SWT.CENTER, false, false)
    }

    // title
    label("Title")
    title = Text(self, SWT.BORDER)
    title.editable = true
    title.layoutData = GridData(SWT.FILL, SWT.CENTER, true, false)
    title.addListener(SWT.Modify, { modified(it) })
    // path
    label("Path")
    path = Text(self, SWT.BORDER)
    path.editable = true
    path.layoutData = GridData(SWT.FILL, SWT.CENTER, true, false)
    path.addListener(SWT.Modify, { modified(it) })
    // type (post vs page)
    label("Type")
    postType = Combo(self, SWT.DROP_DOWN)
    postType.setItems("Page", "Post")
    postType.data = listOf(PostType.PAGE, PostType.BLOGPOST)
    postType.layoutData = GridData(SWT.FILL, SWT.CENTER, true, false)
    postType.addListener(SWT.Selection, { modified(it) })
    // publication status
    publicationStatus = Button(self, SWT.CHECK)
    publicationStatus.text = "Published"
    publicationStatus.layoutData = GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1)
    publicationStatus.addListener(SWT.Selection, { modified(it) })

    label("Publish Date")
    val datePanel = Composite(self, 0)
    datePanel.layoutData = GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1)
    datePanel.layout = RowLayout()
    // publication date
    publishDate = DateTime(datePanel, SWT.DATE)
    publishDate.addListener(SWT.Selection, { modified(it) })
    // publication time
    publishTime = DateTime(datePanel, SWT.TIME)
    publishTime.addListener(SWT.Selection, { modified(it) })

    expander = ExpandItem(expandBar, SWT.FILL, 0)
    expander.expanded = true
    expander.text = "Post details"
    expander.control = self
    expander.height = self.computeSize(SWT.DEFAULT, SWT.DEFAULT).y
    expandBar.addExpandListener(object: ExpandListener {
      override fun itemCollapsed(e: ExpandEvent) {
        resized()
      }
      override fun itemExpanded(e: ExpandEvent) {
        resized()
      }
    })
  }

  fun resized() {
    display.asyncExec({
      expander.height = self.computeSize(SWT.DEFAULT, SWT.DEFAULT).y
      expandBar.requestLayout()
    })
  }

  fun modified(event: Event) {
    val p = post
    if (p == null) return
    p.title = title.text
    p.path = path.text
    p.publishDate = java.time.ZonedDateTime.of(
      publishDate.year,
      publishDate.month + 1,
      publishDate.day,
      publishTime.hours,
      publishTime.minutes,
      0,
      0,
      java.time.ZoneId.systemDefault()
    )
    p.type = (postType.data as List<PostType>)[postType.selectionIndex]
    if (publicationStatus.selection) {
      p.status = PostStatus.PUBLISHED
    } else {
      p.status = PostStatus.DRAFT
    }

    val mod = ModifyEvent(event)
    for (m in listeners) {
      m.modifyText(mod)
    }
  }

  fun addModifyListener(m: ModifyListener) {
    listeners.add(m)
  }

  @Subscribe
  fun fileOpened(f: FileOpened) {
    this.post = null
    val file = f.file
    if (file !is Post) return
    title.text = file.title
    path.text = file.path
    postType.select(0)
    publicationStatus.selection = file.status == PostStatus.PUBLISHED
    val d = file.publishDate
    publishDate.setDate(d.year, d.month.value - 1, d.dayOfMonth)
    publishTime.setTime(d.hour, d.minute, d.second)
    this.post = file
  }
}

@Singleton
class EditView @Inject constructor(
  val ctx: Context,
  val infoView: PostInfoView,
  val display: Display
): View<Composite>() {
  lateinit var bodyEdit: StyledText
  var post: Post? = null
  val listeners = mutableListOf<ModifyListener>()

  override fun build() {
    self = Composite(parent as Composite, SWT.FILL)
    self.setLayout(GridLayout(1, true))
    infoView.parent = self
    infoView.addModifyListener({
      for (listener in listeners) {
        listener.modifyText(it)
      }
    })

    val fonts = display.getFontList("Monospace", true)
    bodyEdit = StyledText(self, SWT.MULTI.or(SWT.V_SCROLL))
    bodyEdit.layoutData = GridData(SWT.FILL, SWT.FILL, true, true)
    bodyEdit.font = Font(display, fonts[0])
    bodyEdit.addModifyListener({
      val p = post
      if (p != null) {
        p.unsavedBody = text
        for (listener in listeners) {
          listener.modifyText(it)
        }
      }
    })
  }

  @Subscribe
  fun beforeSave(evt: BeforeSave) {
    val e = post
    if (e == null) return
    e.body = bodyEdit.text
  }

  @Subscribe
  fun fileOpened(evt: FileOpened) {
    val f = evt.file
    if (f !is Post) {
      return
    }
    post = f
    infoView.post = f
    f.unsavedBody = f.body
    bodyEdit.text = f.body
    bodyEdit.editable = true
  }

  fun addModifyListener(m: ModifyListener) {
    listeners.add(m)
  }

  val text: String
    get() {
      // For some reason, `text` often comes with trailing null bytes
      return bodyEdit.text.replace("\u0000", "")
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
    self.addMouseListener(object: MouseListener {
      override fun mouseDoubleClick(evt: MouseEvent) {
        activate(evt)
      }
      override fun mouseDown(evt: MouseEvent) {}
      override fun mouseUp(evt: MouseEvent) {}
    })
  }

  fun treeItemFromPoint(x: Int, y: Int, list: Array<TreeItem>): TreeItem? {
    for (ti in list) {
      if (ti.bounds.contains(x, y)) {
        return ti
      }
      val child = treeItemFromPoint(x, y, ti.items)
      if (child != null) return child
    }
    return null
  }

  fun activate(evt: MouseEvent) {
    val ti = treeItemFromPoint(evt.x, evt.y, self.items)
    if (ti == null) {
      return
    }
    val data = ti.data
    if (data is BlogFile) {
      bus.post(FileOpened(data))
    }
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

@Singleton
class Preview @Inject constructor(
  val edit: EditView,
): View<Browser>() {
  var blog: Blog? = null
  var post: Post? = null

  override fun build() {
    self = Browser(parent as Composite, SWT.NONE)
    self.javascriptEnabled = false
    // I want a callback that lets me reject attempts to browse outside the correct domain
    // But that seems not to be possible
    // Disabling javascript disables a lot of potential badness
    edit.addModifyListener({ evt -> this.update() })
    showDefaultText()
  }

  fun showDefaultText() {
    self.setText("<html><body>Open a post or page to see a preview of it</body></html>", false)
  }

  @Subscribe
  fun changedBlog(evt: ChangedBlog) {
    blog = evt.blog
    showDefaultText()
  }

  @Subscribe
  fun fileOpened(evt: FileOpened) {
    val f = evt.file
    if (f is Post) {
      post = f
    } else {
      post = null
    }
    update()
  }

  fun update() {
    val b = blog
    val p = post
    if (b == null || p == null) {
      showDefaultText()
      return
    }
    val url = "http://localhost:${SERVER_PORT}${p.path}"
    if (self.url == url) {
      self.refresh()
    } else {
      self.url = url
    }
  }
}

@Singleton
class MainGui @Inject constructor(
    val ctx: Context,
    val shell: Shell,
    val display: Display,
    val mainMenu: MainMenu,
    val fileTree: BlogTreeView,
    val editView: EditView,
    val preview: Preview,
    val persist: Persist,
  ) : ShellAdapter() {
  init {
    shell.text = "PotatoBlog"
    val grid = GridLayout(1, false)
    shell.layout = grid
    val sash = SashForm(shell, SWT.HORIZONTAL)
    sash.layoutData = GridData(SWT.FILL, SWT.FILL, true, true)
    fileTree.parent = sash
    editView.parent = sash
    preview.parent = sash
    sash.setWeights(1, 4, 4)
  }

  fun run(args: Array<String>) {
    shell.open()
    if (args.size == 1) {
      val blog = persist.load(args[0])
      ctx.openBlog(blog, args[0])
    }

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
