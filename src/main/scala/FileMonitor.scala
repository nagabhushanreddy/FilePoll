/**
  * Created by naga on 7/29/17.
  */

import akka.actor.ActorSystem
import com.beachape.filemanagement.MonitorActor
import com.beachape.filemanagement.RegistryTypes._
import com.beachape.filemanagement.Messages._
import java.io.{BufferedWriter, FileWriter}
import java.nio.file.{FileSystems, Paths}
import java.nio.file.StandardWatchEventKinds._
import java.nio.file.WatchEvent.Modifier


import com.sun.nio.file.SensitivityWatchEventModifier





object FileMonitor {

  def main(args:Array[String]): Unit ={

    try {

      //val desktopFile = Paths get "/Users/naga/boa.logo"

      implicit val system = ActorSystem("actorSystem")
      val fileMonitorActor = system.actorOf(MonitorActor(concurrency = 2))

      val modifyCallbackFile: Callback = { path =>
        println(s"Something was modified in a file: $path")
      }

      val modifyCallbackDirectory: Callback = {
        path => println(s"Something was modified in a directory: $path")
      }

      val desktop = Paths get "/Users/naga"
      val desktopFile = Paths get "/Users/naga/my.logo"



      println(s"Checking for modifiations at : $desktop")

      /*
        This will receive callbacks for just the one file
      */
      fileMonitorActor ! RegisterCallback(
        event = ENTRY_MODIFY,
        path = desktopFile,
        callback =  modifyCallbackFile,
        modifier = Some(SensitivityWatchEventModifier.HIGH)
      )

      /*
        If desktopFile is modified, this will also receive a callback
        it will receive callbacks for everything under the desktop directory
      */
      fileMonitorActor ! RegisterCallback(
        event = ENTRY_MODIFY,
        path = desktop,
        callback = modifyCallbackDirectory,
        modifier = Some(SensitivityWatchEventModifier.HIGH)
      )

      //modify a monitored file
      val writer = new BufferedWriter(new FileWriter(desktopFile.toFile))
      writer.append("Naga:Theres text in here wee!!\n")
      writer.close

    } catch {

      case e: IllegalArgumentException => println("illegal arg. exception");
      case e: IllegalStateException    => println("illegal state exception");
      case e             => {
        println("Error:" + e.getMessage);
        e.printStackTrace()
      }

    } finally {
      println("This code is always executed");
      Thread.sleep(30000)
      System.exit(0)
    }
  }

}




