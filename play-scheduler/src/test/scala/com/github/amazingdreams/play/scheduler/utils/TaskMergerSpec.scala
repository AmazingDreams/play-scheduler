package com.github.amazingdreams.play.scheduler.utils

import com.github.amazingdreams.play.scheduler.tasks.{SchedulerTask, TaskInfo}
import org.joda.time.DateTime
import org.scalatestplus.play.PlaySpec

import scala.concurrent.duration._

class TaskMergerSpec extends PlaySpec {

  abstract class FirstSchedulerTask extends SchedulerTask
  abstract class SecondSchedulerTask extends SchedulerTask
  abstract class ThirdSchedulerTask extends SchedulerTask
  abstract class FourthSchedulerTask extends SchedulerTask

  val now = DateTime.now()

  "TaskMerger" should {
    "merge stored and configured tasks" in {
      val configured = Seq(
        TaskInfo(
          taskClass = classOf[FirstSchedulerTask],
          interval = 1 minute
        ),
        TaskInfo(
          taskClass = classOf[SecondSchedulerTask],
          interval = 1 hour
        )
      )

      val stored = Seq(
        TaskInfo(
          taskClass = classOf[FirstSchedulerTask],
          interval = 1 hour,
          lastRunStart = Some(now.minusMinutes(30)),
          created = now.minusDays(3)
        ),
        TaskInfo(
          taskClass = classOf[SecondSchedulerTask],
          interval = 5 hours,
          lastRunStart = Some(now.minusMinutes(60)),
          created = now.minusDays(4)
        )
      )

      val merged = TaskMerger.merge(configured, stored)

      merged.size mustBe 2

      merged(0).taskClass mustBe classOf[FirstSchedulerTask]
      merged(0).interval mustBe (1 minute)
      merged(0).lastRunStart must not be empty
      merged(0).lastRunStart mustBe Some(now.minusMinutes(30))
      merged(0).created mustBe now.minusDays(3)

      merged(1).taskClass mustBe classOf[SecondSchedulerTask]
      merged(1).interval mustBe (1 hour)
      merged(1).lastRunStart must not be empty
      merged(1).lastRunStart mustBe Some(now.minusMinutes(60))
      merged(1).created mustBe now.minusDays(4)
    }

    "add newly configured tasks" in {
      val configured = Seq(
        TaskInfo(
          taskClass = classOf[FirstSchedulerTask],
          interval = 1 minute
        ),
        TaskInfo(
          taskClass = classOf[SecondSchedulerTask],
          interval = 1 hour
        )
      )

      val stored = Seq(
        TaskInfo(
          taskClass = classOf[FirstSchedulerTask],
          interval = 1 hour,
          lastRunStart = Some(now.minusMinutes(30)),
          created = now.minusDays(3)
        )
      )

      val merged = TaskMerger.merge(configured, stored)

      merged.size mustBe 2

      merged(0).taskClass mustBe classOf[FirstSchedulerTask]
      merged(1).taskClass mustBe classOf[SecondSchedulerTask]
    }

    "automatically disable tasks that no longer exist in the configuration" in {
      val configured = Seq(
        TaskInfo(
          taskClass = classOf[FirstSchedulerTask],
          interval = 1 minute
        )
      )

      val stored = Seq(
        TaskInfo(
          taskClass = classOf[FirstSchedulerTask],
          interval = 1 hour,
          lastRunStart = Some(now.minusMinutes(30)),
          created = now.minusDays(3)
        ),
        TaskInfo(
          taskClass = classOf[SecondSchedulerTask],
          interval = 5 hours,
          lastRunStart = Some(now.minusMinutes(60)),
          created = now.minusDays(4)
        )
      )

      val merged = TaskMerger.merge(configured, stored)

      merged.size mustBe 2

      merged(0).taskClass mustBe classOf[FirstSchedulerTask]
      merged(0).isEnabled mustBe true

      merged(1).taskClass mustBe classOf[SecondSchedulerTask]
      merged(1).isEnabled mustBe false
    }
  }
}
