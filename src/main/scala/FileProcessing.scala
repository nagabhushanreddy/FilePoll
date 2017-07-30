import java.io.File
import java.nio.file.{Files, Path, Paths, StandardCopyOption}

import com.typesafe.config._

import scala.collection.JavaConverters._
import java.text.SimpleDateFormat
import java.util.Calendar

/**
  * Created by naga on 7/30/17.
  */

object FileProcessing {
  def main(args: Array[String]): Unit = {
    val fp = new FileProcessing
    println(fp.fileMoves)
    println(fp.dir)
    println("My desired date:"+fp.toDateFormat("Naga%dd-MMM-yyyy%.txt"))
    new DirectoryWatchService(fp.dir).processEvents(Some(fp.actOnWatchEvent))
  }
}

class FileProcessing {

  val conf = ConfigFactory.load()
  val fileMoves = conf.getStringList("fileMoves").asScala
    .toList.map(text => text.split("="))
    .map(a => (a(0) -> a(1))).toMap
  val dir: Path = Paths.get(conf.getString("watchDir"))

  def buzzWhenDone(child: Path, timer: Int = 1000): Boolean = {
    var isGrowing = false;
    var initialWeight = 0.0;
    var finalWeight = 0.0;
    do {
      initialWeight = child.toFile().length();
      Thread.sleep(timer);
      finalWeight = child.toFile().length();
      isGrowing = initialWeight < finalWeight;
    } while (isGrowing);

    System.out.println("Finished creating file!-" + child);
    !isGrowing
  }

  def toDateFormat(name:String):String={
    try{
      val pattern = "(?s)%(.*?)%".r
      val dateFormat= pattern.findFirstIn(name).get.replace("%","")
      val desiredFormat = new SimpleDateFormat(dateFormat)
      val formattedDate = desiredFormat.format(Calendar.getInstance().getTime)
      val formattedName = name.replaceAll(pattern.toString(),formattedDate.toString)
      return formattedName
    }catch{
      case _ => return name
    }
  }

  def actOnWatchEvent(event: String, child: Path):Unit = {
    val destFileName = toDateFormat(fileMoves.getOrElse(child.toString,null))
    println("Found destination file-name " + destFileName + " for " + child + " on " + event)
    destFileName match  {
      case d:String => {
        event match {
          case "ENTRY_CREATE" | "ENTRY_MODIFY"  => {
            val dest = new File(d).toPath
            buzzWhenDone(child)
            println("mv %s %s".format(child,dest))
            Files.move(child, dest, StandardCopyOption.ATOMIC_MOVE)
          }
          case "ENTRY_DELETE" => println ("No Action defined!")
        }
      }
      case _ => {
        println("File "+child.toString+" "+event)
      }
    }
  }
}
