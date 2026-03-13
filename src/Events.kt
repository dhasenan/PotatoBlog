package potatoblog

class ChangedBlog(val blog: Blog)
class PleaseQuit
class ShuttingDown
class FileAdded(val file: BlogFile)
class FileRemoved(val file: BlogFile)
class FileRenamed(val file: BlogFile)
class FileOpened(val file: BlogFile)
