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
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PlayScheduler @Inject()(actorSystem: ActorSystem,
                              injector: Injector,
                              configuration: PlaySchedulerConfiguration,
                              persistence: PlaySchedulerPersistence)
                             (implicit ec: ExecutionContext) {

  val logger = Logger(getClass)
  val scheduler = actorSystem.scheduler

  if (configuration.isEnabled) {
    startup()
  }

  def configuredTasks = configuration.readTasks()

  def getTasks(): Future[Seq[TaskInfo]] = persistence.getTasks()

  private def startup(): Future[Unit] = {
    loadAndPersistInitialTasks().map { initialTasks =>
      // Schedule now
      scheduleCron()
    }
  }

  private def cron() = {
    runScheduledTasks()
    scheduleCron()
  }

  private def scheduleCron(): Cancellable = {
    scheduler.scheduleOnce(configuration.schedulerInterval) {
      cron()
    }
  }

  private def loadAndPersistInitialTasks(): Future[Seq[TaskInfo]] =
    persistence.getTasks().flatMap { persisted =>
      Logger.debug(s"persisted: ${persisted.size}")
      Logger.debug(s"configured: ${configuredTasks.size}")

      Future.sequence {
        TaskMerger.merge(configuredTasks, persisted)
          .map(persistence.persist)
      }
    }

  private def runScheduledTasks(): Future[Unit] =
    persistence.getTasksToBeExecuted().map { tasks =>
      tasks.map { taskInfo =>
        // Run task
        scheduler.scheduleOnce(0 seconds) {
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
            persistence.persist(updatedTask.copy(
              isRunning = false,
              lastRunResult = Some(result)
            ))
          }
        } catch {
          case e: Throwable =>
            Logger.error("Fatal error during task execution: ", e)
            persistence.persist(updatedTask.copy(
              isEnabled = false,
              isRunning = false,
              lastRunResult = Some("FATAL ERROR")
            ))
        }
    } yield (finishedTask)
}
