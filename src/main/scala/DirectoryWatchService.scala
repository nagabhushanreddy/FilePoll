/**
  * Created by naga on 7/30/17.
  */

import java.nio.file.StandardWatchEventKinds._
import java.io.IOException
import java.nio.file.FileSystems
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.SimpleFileVisitor
import java.nio.file.WatchEvent
import java.nio.file.WatchKey
import java.nio.file.WatchService
import java.nio.file.attribute.BasicFileAttributes
import java.util.{Calendar, HashMap, Map}

//remove if not needed
import scala.collection.JavaConversions._

object DirectoryWatchService {

  def main(args: Array[String]): Unit = {
    val dir: Path = Paths.get("/Users/naga/temp")
    new DirectoryWatchService(dir).processEvents(None)
  }

}


/**
  * Creates a WatchService and registers the given directory
  */

class DirectoryWatchService(dir: Path) {

  private val watcher: WatchService = FileSystems.getDefault.newWatchService()

  private val keys: Map[WatchKey, Path] = new HashMap[WatchKey, Path]()

  walkAndRegisterDirectories(dir)

  /**
    * Register the given directory with the WatchService; This function will be called by FileVisitor
    */
  private def registerDirectory(dir: Path): Unit =
  {
    val key: WatchKey =
      dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)
    keys.put(key, dir)
  }

  /**
    * Register the given directory, and all its sub-directories, with the WatchService.
    */
  private def walkAndRegisterDirectories(start: Path): Unit =
  {
    // register directory and sub-directories
    Files.walkFileTree(
      start,
      new SimpleFileVisitor[Path]() {
        override def preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult = {
          registerDirectory(dir)
          FileVisitResult.CONTINUE
        }
      }
    )
  }

  /**
    * Process all events for keys queued to the watcher
    */
  def processEvents(optionalMethod: Option[(String, Path) => Unit]): Unit = {

    while (true) {

      System.out.format( Calendar.getInstance().getTime + ":Checking for Events\n")

      // wait for key to be signalled
      var key: WatchKey = null
      try key = watcher.take()
      catch {
        case x: InterruptedException => return

      }
      val dir: Path = keys.get(key)
      if (dir == null) {
        System.err.println("WatchKey not recognized!!")
        //continue
      }

      for (event <- key.pollEvents()) {
        val kind= event.kind()
        // Context for directory entry event is the file name of entry
        val name: Path = event.asInstanceOf[WatchEvent[Path]].context()
        val child: Path = dir.resolve(name)

        // print out event


        optionalMethod match {
          case Some(f) => {
            f(event.kind().name(),child)
          }
          case None =>  System.out.format( Calendar.getInstance().getTime + ":%s: %s\n", event.kind().name(), child)

        }


        

        // if directory is created, and watching recursively, then register it and its sub-directories
        if (kind == ENTRY_CREATE) {
          try if (Files.isDirectory(child)) {
            walkAndRegisterDirectories(child)
          } catch {
            case x: IOException => {}

          }
        }
      }

      // reset key and remove from set if directory no longer accessible
      val valid: Boolean = key.reset()
      if (!valid) {
        keys.remove(key)
        // all directories are inaccessible
        if (keys.isEmpty) {
          //break
        }
      }

    }

  }

}