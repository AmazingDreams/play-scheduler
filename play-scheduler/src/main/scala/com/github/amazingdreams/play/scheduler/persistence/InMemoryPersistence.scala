package com.github.amazingdreams.play.scheduler.persistence

import javax.inject.{Inject, Singleton}

import com.github.amazingdreams.play.scheduler.tasks.{SchedulerTask, TaskInfo}

import scala.collection.mutable.{HashMap => MutableMap}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class InMemoryPersistence @Inject()()(implicit val ec: ExecutionContext) extends PlaySchedulerPersistence {

  private val taskList: MutableMap[Class[_ <: SchedulerTask], TaskInfo] = MutableMap.empty

  override def getTasks: Future[Seq[TaskInfo]] =
    taskList.synchronized {
      Future {
        taskList.values.toSeq
      }
    }

  override def getTasksToBeExecuted(): Future[Seq[TaskInfo]] =
    taskList.synchronized {
      Future {
        taskList.values.filter { taskInfo =>
          !taskInfo.isRunning &&
            taskInfo.isEnabled &&
            taskInfo.nextRun.isBeforeNow
        }.toSeq
      }
    }

  override def persist(taskInfo: TaskInfo): Future[TaskInfo] =
    taskList.synchronized {
      Future {
        taskList.put(taskInfo.taskClass, taskInfo)
        taskInfo
      }
    }
}
