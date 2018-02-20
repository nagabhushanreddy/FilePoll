import java.io.File
import java.nio.file.{FileSystem, Path}


sealed trait FileSystemChange

case class Created(fileOrDir: File) extends FileSystemChange
case class Deleted(fileOrDir: File) extends FileSystemChange
case class Modified(fileOrDir: File) extends FileSystemChange
case class MonitorDir(path: Path)
case class Existing(path: Path)

case class Archive(path: Path)
