

import java.io.{BufferedReader, File, FileOutputStream}
import java.nio.file.{Files, Path, Paths, StandardCopyOption}

import com.typesafe.config._

import scala.collection.JavaConverters._
import java.text.SimpleDateFormat
import java.util.Calendar

import com.typesafe.scalalogging.LazyLogging
import java.util.zip.{ZipEntry, ZipOutputStream}

import scala.io.{Codec, Source}
import com.github.nscala_time.time.Imports._
import org.joda.time.format.DateTimeFormat

/**
  * Created by naga on 7/30/17.
  */

object FileProcessing extends LazyLogging {

  def main(args: Array[String]): Unit = {
    val fp = new FileProcessing
    logger.info("My desired date:" + fp.toDateFormat("Naga%dd-MMM-yyyy%.txt"))
    logger.info("Watch-Serivce started...")
  }
}

class FileProcessing extends LazyLogging {
  val conf = ConfigFactory.load()
  val fileMoves = conf.getStringList("fileMoves").asScala
    .toList.map(text => text.split("="))
    .map(a => (a(0) -> a(1))).toMap
  val fileWorkFlows = conf.getConfigList("fileWorkflow").asScala

  val archEnabled = if (conf.hasPath("archive.enabled")) conf.getString("archive.enabled") else "NO"
  val archDays: Int = if (conf.hasPath("archive.archiveDays")) conf.getInt("archive.archiveDays") else 0
  val zipNameFormat = if (conf.hasPath("archive.zipNameForma")) conf.getString("archive.zipNameFormat") else "%YYYY-MMM%"
  val zipCodec = if (conf.hasPath("archive.zipCodec")) conf.getString("archive.zipCodec") else "UTF-8"
  val archiveMethod = if (conf.hasPath("archive.archiveMethod")) conf.getString("archive.archiveMethod") else "MONTHLY"
  val runTime = if (conf.hasPath("AppRunTime")) conf.getInt("AppRunTime") else 1
  val dir: Path = Paths.get(conf.getStringList("watchDirs").get(0))


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
    logger.info("Finished creating file!-" + child);
    !isGrowing
  }

  def toDateFormat(name: String): String = {
    try {
      val pattern = "(?s)%(.*?)%".r
      val dateFormat = pattern.findFirstIn(name).get.replace("%", "")
      val desiredFormat = new SimpleDateFormat(dateFormat)
      val formattedDate = desiredFormat.format(Calendar.getInstance().getTime)
      val formattedName = name.replaceAll(pattern.toString(), formattedDate.toString)
      return formattedName
    } catch {
      case _: Throwable => return name
    }
  }

  def toDateFormat(name: String, dateTime: DateTime): String = {
    try {
      val pattern = "(?s)%(.*?)%".r
      val dateFormat = pattern.findFirstIn(name).get.replace("%", "")
      val fmt = DateTimeFormat.forPattern(dateFormat)
      val formattedDate = fmt.print(dateTime)
      val formattedName = name.replaceAll(pattern.toString(), formattedDate.toString)
      return formattedName
    } catch {
      case _: Throwable => return name
    }

  }

  def checkAndMoveToDest(path: Path) = {

    logger.debug("Check config for ... " + path.toString)

    //val destFileName = toDateFormat(fileMoves.getOrElse(path.toString, null))
    val k = fileMoves.keys.find((path.toString).matches(_)).getOrElse(null)
    // val destFileName = toDateFormat(fileMoves.getOrElse(k, null))
    var destFileName = fileMoves.getOrElse(k, null)

    logger.debug("\n" + path.toString + "->" + k + "->" + destFileName)

    destFileName match {
      case d: String => {
        buzzWhenDone(path)
        try {
          var dest = toDateFormat(d, path.toFile.lastModified().toDateTime)
          var counter = 0
          var chk = dest
          while (Files.exists(Paths.get(chk)) && counter < 2000) {
            counter += 1
            chk = dest.replaceAll("\\.(?=[^.]*$)", "_" + counter + ".")
          }
          dest = chk
          logger.info("mv %s %s".format(path, dest))
          Files.move(path, new File(dest).toPath, StandardCopyOption.ATOMIC_MOVE)
        } catch {
          case e: Throwable => logger.error("Unable to move " + path.toString + " - " + e.getMessage)
        }
      }
      case _ => {
        logger.debug("No destination found for " + path)
      }
    }
  }


  def fileWorkflow(path: Path) = {
    val matchedWorflows = fileWorkFlows.toList.filter(p => {
      if (p.hasPath("sourceFile"))
        (path.toString).matches(p.getString("sourceFile"))
      else false
    })
    matchedWorflows.par.foreach(w=>{
      {if (w.hasPath("actionMethod")) w.getString("actionMethod")  else "" } match {
        case "printFileData" => logger.info("I will print file Data")
        case "checkAndMoveToDest"=> logger.info("I will move file")
        case "" => logger.info("No Action required.")
      }
    })

  }


  private def zip(out: Path, files: Iterable[Path]) = {
    val zip = new ZipOutputStream(Files.newOutputStream(out))
    try {
      files.foreach { file =>
        zip.putNextEntry(new ZipEntry(file.toString))
        Files.copy(file, zip)
        zip.closeEntry()
        logger.info("zipped file " + file)
      }
    } finally {
      zip.close()
    }
  }

  def compress(zipFilepath: String, files: List[File]) {
    def readByte(bufferedReader: BufferedReader): Stream[Int] = {
      bufferedReader.read() #:: readByte(bufferedReader)
    }

    val zip = new ZipOutputStream(new FileOutputStream(zipFilepath))
    try {
      for (file <- files) {
        //add zip entry to output stream
        logger.info("zipped file " + file)
        var errorFlag = 0
        zip.putNextEntry(new ZipEntry(file.getName))
        val in = Source.fromFile(file)(Codec(zipCodec)).bufferedReader()
        try {
          readByte(in).takeWhile(_ > -1).toList.foreach(zip.write(_))
        } catch {
          case e: Throwable => logger.error(e.toString)
            errorFlag = 1
        }
        finally {
          in.close()
        }
        zip.closeEntry()
        if (errorFlag == 0)
          file.deleteOnExit()
      }
    } catch {
      case e: Throwable => logger.error(e.toString)
    }
    finally {
      zip.close()
    }
  }


  def archiveFiles = {
    val dirsToArchive = fileMoves.map(_._2).map(x => {
      Paths.get(x).getParent.toString
    }).toList.distinct
    // val archiveDate =  new Date(new Date().getTime - archDays * 24 * 3600 * 1000 )
    val archiveDate = archiveMethod match {
      case "MONTHLY" => (DateTime.now() - archDays.days).hour(0).minute(0).second(0).day(1)
      case "WEEKLY" => (DateTime.now() - archDays.days).hour(0).minute(0).second(0).dayOfWeek().withMinimumValue
      case _ => (DateTime.now() - archDays.days).hour(0).minute(0).second(0)
    }
    logger.info("Archive start Date:" + archiveDate.toString())
    dirsToArchive.foreach(dir => {
      val thisDir = new File(dir)
      if (thisDir.isDirectory) {
        val fileList = thisDir.listFiles().filter(f => {
          f.lastModified().toDateTime < archiveDate
        })
        val archiveDir = new File(thisDir.getPath + "//ARCHIVE")
        if (!(archiveDir.exists() && archiveDir.isDirectory)) {
          logger.info("Creating archive directory " + archiveDir)
          archiveDir.mkdir()
        }
        var counter = 0
        var zipName: Path = Paths.get(toDateFormat(archiveDir.getPath + "//" + zipNameFormat + "_" + counter + ".zip", archiveDate))
        while (Files.exists(zipName)) //have to get alpha month
        {
          counter += 1
          zipName = Paths.get(toDateFormat(archiveDir.getPath + "//" + zipNameFormat + "_" + counter + ".zip", archiveDate))
        }
        logger.info("Creating archive " + zipName.toString + " for " + fileList.toList.toString)
        if (!fileList.isEmpty)
          try {
            compress(zipName.toString, fileList.toList)
          } catch {
            case e: Throwable => logger.error("Unable to archive folder " + thisDir.getPath + "!!")
          }
      }
    })
  }

  def moveExistingFiles(watchDir: Path): Unit = {
    try {
      logger.info("Moving existing files from  " + watchDir)
      val fileList = new File(watchDir.toString).listFiles()
      logger.debug("Received file List, number of files " + fileList.length)
      fileList.foreach(f => checkAndMoveToDest(f.toPath))
    } catch {
      case e: Throwable => logger.error("Error while moving existing!" + e.getMessage)
    }

  }

  def actOnWatchEvent(event: String, child: Path): Unit = {
    event match {
      case "ENTRY_CREATE" | "ENTRY_MODIFY" => {
        checkAndMoveToDest(child)
      }
      case "ENTRY_DELETE" => logger.warn("No Action defined for %s!".format(event))
    }
  }


}

