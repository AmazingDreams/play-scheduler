package com.github.amazingdreams.play.scheduler.tasks

import org.joda.time.DateTime
import org.scalatestplus.play.PlaySpec

import scala.concurrent.duration._

class TaskInfoSpec extends PlaySpec {

  "TaskInfo" should {
    "calculate next run time based on initial delay" in {
      val now = DateTime.now()

      val taskInfo = TaskInfo(
        taskClass = classOf[SchedulerTask],
        initialDelay = 10 minutes,
        interval = 10 minutes,
        created = now
      )

      taskInfo.nextRun() mustBe now.plusMinutes(10)
    }

    "calculate next run time based on interval" in {
      val now = DateTime.now()

      val taskInfo = TaskInfo(
        taskClass = classOf[SchedulerTask],
        interval = 5 minutes,
        lastRun = Some(now),
        created = now.minusDays(1)
      )

      taskInfo.nextRun() mustBe now.plusMinutes(5)
    }
  }
}
