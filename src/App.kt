package potatoblog

import org.eclipse.swt.*
import org.eclipse.swt.browser.*
import org.eclipse.swt.custom.*
import org.eclipse.swt.layout.*
import org.eclipse.swt.widgets.*

fun main() {
  val display = Display()
  val shell = Shell(display)
  shell.text = "PotatoBlog"
  val shellRow = RowLayout()
  shellRow.fill = true
  shellRow.justify = true
  shell.layout = shellRow
  val menubar = Menu(shell, SWT.BAR)
  shell.menuBar = menubar

  // TODO build main menu
  val fileItem = MenuItem(menubar, SWT.PUSH)
  val fileMenu = Menu(menubar)
  fileItem.text = "&File"
  shell.open()

  val mainPane = Composite(shell, SWT.FILL)
  val mainRow = FillLayout(SWT.HORIZONTAL)
  mainRow.type = SWT.HORIZONTAL
  //mainRow.fill = true
  shellRow.justify = true
  mainPane.layout = mainRow
  val fileTree = Tree(mainPane, SWT.SINGLE)
  val fileSash = Sash(mainPane, SWT.VERTICAL)
  val editor = StyledText(mainPane, SWT.WRAP or SWT.FILL)
  val preview = Browser(mainPane, SWT.FILL)
  preview.url = "https://example.org"

  while (!shell.isDisposed()) {
    if (!display.readAndDispatch()) {
      display.sleep()
    }
  }
  display.dispose()
}
