package com.github.amazingdreams.play.scheduler.persistence

import com.github.amazingdreams.play.scheduler.tasks.TaskInfo

import scala.concurrent.Future

trait PlaySchedulerPersistence {
  def getTasks(): Future[Seq[TaskInfo]]
  def getTasksToBeExecuted(): Future[Seq[TaskInfo]]

  def persist(taskInfo: TaskInfo): Future[TaskInfo]
}
