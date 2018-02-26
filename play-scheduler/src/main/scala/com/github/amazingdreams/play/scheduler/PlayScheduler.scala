package com.github.amazingdreams.play.scheduler

import javax.inject.{Inject, Singleton}

import akka.actor.{ActorSystem, Cancellable}
import com.github.amazingdreams.play.scheduler.module.PlaySchedulerConfiguration
import com.github.amazingdreams.play.scheduler.persistence.PlaySchedulerPersistence
import com.github.amazingdreams.play.scheduler.tasks.{SchedulerTask, TaskInfo}
import com.github.amazingdreams.play.scheduler.utils.TaskMerger
import org.joda.time.DateTime
import play.api.Logger
import play.api.inject.Injector

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

@Singleton
class PlayScheduler @Inject()(actorSystem: ActorSystem,
                              injector: Injector,
                              configuration: PlaySchedulerConfiguration,
                              persistence: PlaySchedulerPersistence)
                             (implicit ec: ExecutionContext) {

  val logger = Logger(getClass)
  val scheduler = actorSystem.scheduler
  val configuredTasks = configuration.readTasks()

  if (configuration.isEnabled) {
    startup()
  }

  def getTasks(): Future[Seq[TaskInfo]] = persistence.getTasks()

  private def startup(): Future[Unit] = {
    Await.result(loadAndPersistInitialTasks().map { initialTasks =>
      // Schedule now
      Future(scheduleCron())
    }, 3 seconds)
  }

  private def cron() = {
    logger.debug("Starting task run")

    runScheduledTasks().map { ranTasks =>
      scheduleCron()
    }
  }

  private def scheduleCron(): Cancellable = {
    logger.debug(s"Scheduling running tasks every ${configuration.schedulerInterval.length} ${configuration.schedulerInterval.unit}")

    scheduler.scheduleOnce(configuration.schedulerInterval) {
      cron()
    }
  }

  private def loadAndPersistInitialTasks(): Future[Seq[TaskInfo]] =
    persistence.getTasks().flatMap { persisted =>
      logger.debug(s"Found ${persisted.size} persisted tasks")
      logger.debug(s"Found ${configuredTasks.size} configured tasks")

      Future.sequence {
        try {
          TaskMerger.merge(configuredTasks, persisted)
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
