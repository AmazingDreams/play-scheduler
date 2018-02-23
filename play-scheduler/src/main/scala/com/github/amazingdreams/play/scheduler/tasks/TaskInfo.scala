package com.github.amazingdreams.play.scheduler.tasks

import org.joda.time.DateTime

import scala.concurrent.duration._

case class TaskInfo(taskClass: Class[_ <: SchedulerTask],
                    interval: FiniteDuration,
                    initialDelay: FiniteDuration = 0 seconds,
                    isEnabled: Boolean = true,
                    isRunning: Boolean = false,
                    lastRunResult: Option[String] = None,
                    lastRun: Option[DateTime] = None,
                    created: DateTime = DateTime.now()) {

  def nextRun(): DateTime = lastRun match {
    case Some(lastRun) => lastRun.plusSeconds(interval.toSeconds.toInt)
    case None => created.plusSeconds(initialDelay.toSeconds.toInt)
  }
}
