package com.github.amazingdreams.play.scheduler

import akka.actor.{Actor, Cancellable}
import com.github.amazingdreams.play.scheduler.module.PlaySchedulerConfiguration
import com.github.amazingdreams.play.scheduler.persistence.PlaySchedulerPersistence
import com.github.amazingdreams.play.scheduler.tasks.{SchedulerTask, TaskInfo}
import com.github.amazingdreams.play.scheduler.utils.TaskMerger
import org.joda.time.DateTime
import play.api.Logger
import play.api.inject.Injector

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

object PlayScheduler {
  final val ACTOR_NAME = "PlaySchedulerActor"
  final val PROXY_NAME = "PlaySchedulerProxyActor"

  def Props(injector: Injector,
            configuration: PlaySchedulerConfiguration,
            persistence: PlaySchedulerPersistence)
           (implicit ec: ExecutionContext) =
    akka.actor.Props(classOf[PlayScheduler], injector, configuration, persistence, ec)

  case object Start
  case object Stop
  case object RunTasks
  case class RunTask(task: TaskInfo)
}

class PlayScheduler(injector: Injector,
                    configuration: PlaySchedulerConfiguration,
                    persistence: PlaySchedulerPersistence)
                   (implicit ec: ExecutionContext)
  extends Actor {
  import PlayScheduler._

  val logger = Logger(getClass)
  val scheduler = context.system.scheduler

  var cancellable: Cancellable = null

  override def preStart(): Unit =
    self ! Start

  override def receive: Receive = {
    case Start =>
      logger.debug("Received Start message")

      loadAndPersistInitialTasks().map { initialTasks =>
        // Schedule now
        cancellable = scheduleCron()
      }
    case Stop =>
      logger.debug("Received Stop message")

      if (cancellable != null) {
        cancellable.cancel()
      }
    case RunTasks =>
      logger.debug("Received RunTasks message")

      runScheduledTasks().map { ranTasks =>
         cancellable = scheduleCron()
      }
    case RunTask(task) =>
      logger.debug(s"Received RunTask(${task.taskClass}) message")

      runTask(task)
  }

  def configuredTasks() = configuration.readTasks()
  def persistedTasks() = persistence.getTasks()

  private def scheduleCron(): Cancellable = {
    logger.debug(s"Scheduling running tasks every" +
      s" ${configuration.schedulerInterval.length}" +
      s" ${configuration.schedulerInterval.unit}")

    scheduler.scheduleOnce(configuration.schedulerInterval) {
      self ! RunTasks
    }
  }

  private def loadAndPersistInitialTasks(): Future[Seq[TaskInfo]] = {
    val configured = configuredTasks()
    logger.debug(s"Found ${configured.size} configured tasks")

    persistedTasks().flatMap { persisted =>
      logger.debug(s"Found ${persisted.size} persisted tasks")

      Future.sequence {
        try {
          TaskMerger.merge(configured, persisted)
            .map(persistence.persist)
        } catch {
          case e: Throwable =>
            logger.error("Error during initial task setup", e)
            throw e
        }
      }.map { result =>
        logger.debug(s"Tasks after merge: ${result.size}")
        result
      }
    }
  }

  private def runScheduledTasks(): Future[Seq[TaskInfo]] =
    persistence.getTasksToBeExecuted().map { tasks =>
      tasks.map { taskInfo =>
        self ! RunTask(taskInfo)

        taskInfo
      }
    }

  private def runTask(taskInfo: TaskInfo): Future[TaskInfo] = {
    val startTime = DateTime.now()

    for {
      updatedTask <- persistence.persist(taskInfo.copy(
        lastRunStart = Some(startTime),
        lastRunEnd = None,
        nextRun = startTime.plusSeconds(taskInfo.interval.toSeconds.toInt),
        isRunning = true
      ))
      finishedTask <-
        try {
          val instance: SchedulerTask = injector.instanceOf(updatedTask.taskClass)
          instance.run().flatMap { result =>
            logger.debug(s"Running task ${taskInfo.taskClass} success!")

            persistence.persist(updatedTask.copy(
              lastRunEnd = Some(DateTime.now()),
              lastRunResult = Some(result),
              isRunning = false
            ))
          }
        } catch {
          case e: Throwable =>
            logger.error("Fatal error during task execution: ", e)

            persistence.persist(updatedTask.copy(
              isEnabled = false,
              isRunning = false,
              lastRunEnd = Some(DateTime.now()),
              lastRunResult = Some("FATAL ERROR")
            )).map { finished =>
              finished
            }
        }
    } yield {
      logger.debug(s"Task ${finishedTask.taskClass} exited with: ${finishedTask.lastRunResult}")
      logger.debug(s"Successfully stored result of ${finishedTask.taskClass}")

      finishedTask
    }
  }
}
