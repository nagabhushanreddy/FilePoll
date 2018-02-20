import java.nio.file._
import java.util.concurrent.TimeUnit

import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import it.sauronsoftware.junique.{AlreadyLockedException, JUnique}
import java.lang.management.ManagementFactory


object WatchMain extends App with LazyLogging {

  val conf = ConfigFactory.load()
  val runTime = if (conf.hasPath("AppRunTime")) conf.getInt("AppRunTime") else 1
  var appId = if (conf.hasPath("AppName")) conf.getString("AppName") else "WATCH-SERVICE"
  val watchDirs = conf.getStringList("watchDirs")
  val myPID = ManagementFactory.getRuntimeMXBean.getName

  var  alreadyRunning=false
  try {
    JUnique.acquireLock(appId);
    logger.info(s"Instance running check completed! Looks OK. $myPID")
  } catch {
    case e:AlreadyLockedException =>  alreadyRunning = true;
  }
  if (alreadyRunning) {
    logger.error(appId + s" already running! This instance $myPID will exit.")
    System.exit(1)
  }

  val system = ActorSystem("WatchFsSystem")
  logger.info("Started")
  val fsActor = system.actorOf(Props[FileSystemActor], "fileSystem")

  watchDirs.forEach(dir => {
    val watchDir: Path = Paths.get(dir)
    fsActor ! MonitorDir(watchDir)
  })

  logger.info("Watch Service will Terminate after %s minutes".format(runTime))
  TimeUnit.MINUTES.sleep(runTime)
  system.terminate()

}

