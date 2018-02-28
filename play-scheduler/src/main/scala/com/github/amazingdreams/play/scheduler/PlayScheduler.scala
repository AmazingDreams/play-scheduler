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
  }

  def configuredTasks() = configuration.readTasks()
  def persistedTasks() = persistence.getTasks()

  private def scheduleCron(): Cancellable = {
    logger.debug(s"Scheduling running tasks every ${configuration.schedulerInterval.length} ${configuration.schedulerInterval.unit}")

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
        // Run task
        scheduler.scheduleOnce(0 seconds) {
          logger.debug(s"Running task ${taskInfo.getClass}")
          runTask(taskInfo)
        }

        taskInfo
      }
    }

  private def runTask(taskInfo: TaskInfo): Future[TaskInfo] =
    for {
      updatedTask <- persistence.persist(taskInfo.copy(
        lastRun = Some(DateTime.now()),
        isRunning = true
      ))
      finishedTask <-
        try {
          val instance: SchedulerTask = injector.instanceOf(updatedTask.taskClass)
          instance.run().flatMap { result =>
            logger.debug(s"Running task ${taskInfo.getClass} success!")

            persistence.persist(updatedTask.copy(
              isRunning = false,
              lastRunResult = Some(result)
            )).map { finished =>
              logger.debug(s"Successfully stored result of ${finished.getClass}")
              finished
            }
          }
        } catch {
          case e: Throwable =>
            logger.error("Fatal error during task execution: ", e)

            persistence.persist(updatedTask.copy(
              isEnabled = false,
              isRunning = false,
              lastRunResult = Some("FATAL ERROR")
            )).map { finished =>
              logger.debug(s"Successfully stored result of ${finished.getClass}")
              finished
            }
        }
    } yield (finishedTask)
}
