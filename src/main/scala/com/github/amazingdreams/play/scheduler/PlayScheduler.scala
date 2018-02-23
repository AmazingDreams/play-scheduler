package com.github.amazingdreams.play.scheduler

import javax.inject.{Inject, Singleton}

import akka.actor.{ActorSystem, Cancellable}
import com.github.amazingdreams.play.scheduler.module.PlaySchedulerConfiguration
import com.github.amazingdreams.play.scheduler.persistence.PlaySchedulerPersistence
import com.github.amazingdreams.play.scheduler.tasks.{SchedulerTask, TaskInfo}
import com.github.amazingdreams.play.scheduler.utils.TaskMerger
import org.joda.time.DateTime
import play.api.inject.Injector

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PlayScheduler @Inject()(actorSystem: ActorSystem,
                              injector: Injector,
                              configuration: PlaySchedulerConfiguration,
                              persistence: PlaySchedulerPersistence)
                             (implicit ec: ExecutionContext) {

  val scheduler = actorSystem.scheduler

  if (configuration.isEnabled) {
    startup()
  }

  def startup() = {
    loadAndPersistInitialTasks().map { intitialTasks =>
      // Schedule now
      scheduleCron()
    }
  }

  def cron() = {
    runScheduledTasks()
    scheduleCron()
  }

  def scheduleCron(): Cancellable = {
    scheduler.scheduleOnce(configuration.schedulerInterval) {
      cron()
    }
  }

  def loadAndPersistInitialTasks(): Future[Seq[TaskInfo]] =
    persistence.getTasks().flatMap { persisted =>
      val configured = configuration.readTasks()

      Future.sequence {
        TaskMerger.merge(configured, persisted)
          .map(persistence.persist)
      }
    }

  def runScheduledTasks(): Future[Seq[TaskInfo]] =
    persistence.getTasksToBeExecuted().flatMap { tasks =>
      Future.sequence {
        tasks.map { taskInfo =>
          runTask(taskInfo)
        }
      }
    }

  def runTask(taskInfo: TaskInfo): Future[TaskInfo] = {
    for {
      updatedTask <- persistence.persist(taskInfo.copy(
        lastRun = Some(DateTime.now()),
        isRunning = true
      ))
      runResult <- {
        val instance: SchedulerTask = injector.instanceOf(updatedTask.taskClass)
        instance.run()
      }
      finishedTask <- persistence.persist(taskInfo.copy(
        lastRunResult = Some(runResult)
      ))
    } yield (finishedTask)
  }
}
