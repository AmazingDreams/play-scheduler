package com.github.amazingdreams.play.scheduler

import javax.inject.{Inject, Singleton}

import com.github.amazingdreams.play.scheduler.persistence.PlaySchedulerPersistence
import com.github.amazingdreams.play.scheduler.tasks.TaskInfo

import scala.concurrent.Future

@Singleton
class PlaySchedulerManager @Inject()(playSchedulerPersistence: PlaySchedulerPersistence) {

  def getTasks(): Future[Seq[TaskInfo]] =
    playSchedulerPersistence.getTasks()

}
