

import java.io.File
import java.nio.file.{Files, StandardCopyOption}
import java.util.concurrent.TimeUnit

import WatchMain.runTime
import akka.actor.Actor
import akka.event.{Logging, LoggingReceive}



class FileSystemActor extends FileProcessing with Actor  {

  val watchServiceTask = new WatchServiceTask(self)
  val watchThread = new Thread(watchServiceTask, "WatchService")

  override def preStart() {
    watchThread.setDaemon(true)
    watchThread.start()
  }

  override def postStop() {
    watchThread.interrupt()
  }

  def receive = LoggingReceive {
    case MonitorDir(path) => {
      logger.info("Monitoring " + path.toString)
      watchServiceTask watchRecursively path
      self ! Existing(path)
      self ! Archive(path)
      self ! "ALIVE"
    }
    case Created(file) => fileWorkflow(file.toPath)
    case Modified(file) => fileWorkflow(file.toPath)
    case Deleted(fileOrDir) => logger.warn("No Action defined for delete!" + fileOrDir.toString)
    case Existing(fileOrDir) => moveExistingFiles(fileOrDir)
    case Archive(fileOrDir) => archEnabled match {
      case "YES" => archiveFiles
      case _ => logger.warn("Archive not enabled!!")
    }
    case "ALIVE" => {
      val aliveThread = new Thread {
        override def run {
          while(true){
            logger.info("This instance is alive!!")
            Thread.sleep(60000)
          }
        }
      }
      aliveThread.start()
    }

  }


}
