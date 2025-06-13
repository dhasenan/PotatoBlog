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
  val grid = GridLayout(1, false)
  shell.layout = grid
  val menubar = Menu(shell, SWT.BAR)
  shell.menuBar = menubar

  // TODO build main menu
  val fileItem = MenuItem(menubar, SWT.PUSH)
  val fileMenu = Menu(menubar)
  fileItem.text = "&File"

  val sash = SashForm(shell, SWT.HORIZONTAL)
  sash.layoutData = GridData(SWT.FILL, SWT.FILL, true, true)
  val fileTree = Tree(sash, SWT.SINGLE)
  val editor = StyledText(sash, SWT.WRAP or SWT.FILL)
  val preview = Browser(sash, SWT.FILL)
  preview.url = "https://example.org"
  sash.setWeights(1, 4, 4)

  shell.open()

  while (!shell.isDisposed()) {
    if (!display.readAndDispatch()) {
      display.sleep()
    }
  }
  display.dispose()
}
