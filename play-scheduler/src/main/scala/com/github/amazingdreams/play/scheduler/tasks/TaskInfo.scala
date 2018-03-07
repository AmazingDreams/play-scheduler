package com.github.amazingdreams.play.scheduler.tasks

import org.joda.time.DateTime

import scala.concurrent.duration._

case class TaskInfo(taskClass: Class[_ <: SchedulerTask],
                    interval: FiniteDuration,
                    initialDelay: FiniteDuration = 0 seconds,
                    isEnabled: Boolean = true,
                    isRunning: Boolean = false,
                    lastRunResult: Option[String] = None,
                    lastRunStart: Option[DateTime] = None,
                    lastRunEnd: Option[DateTime] = None,
                    nextRun: DateTime = DateTime.now(),
                    created: DateTime = DateTime.now())
