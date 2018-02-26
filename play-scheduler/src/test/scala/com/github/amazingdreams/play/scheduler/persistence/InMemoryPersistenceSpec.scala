package com.github.amazingdreams.play.scheduler.persistence

import com.github.amazingdreams.play.scheduler.tasks.{SchedulerTask, TaskInfo}
import org.joda.time.DateTime
import org.scalatestplus.play.PlaySpec
import play.api.test.Helpers._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

class InMemoryPersistenceSpec extends PlaySpec {

  abstract class FirstTestingSchedulerClass extends SchedulerTask
  abstract class SecondTestingSchedulerClass extends SchedulerTask

  "InMemoryPersistence" should {
    "persist task information" in {
      val persistence = new InMemoryPersistence()

      await(Future.sequence(Seq(
        persistence.persist(TaskInfo(
          taskClass = classOf[FirstTestingSchedulerClass],
          interval = 1 minute
        )),
        persistence.persist(TaskInfo(
          taskClass = classOf[SecondTestingSchedulerClass],
          interval = 1 minute
        ))
      )))

      val tasks = await(persistence.getTasks)
      tasks.size mustBe 2
    }

    "get tasks that should be executed based initial delay" in {
      val persistence = new InMemoryPersistence()

      val task1 = TaskInfo(
        taskClass = classOf[FirstTestingSchedulerClass],
        interval = 1 minute,
        initialDelay = 0 second
      )
      val task2 = TaskInfo(
        taskClass = classOf[SecondTestingSchedulerClass],
        interval = 1 minute,
        initialDelay = 10 minutes
      )

      await(Future.sequence(Seq(
        persistence.persist(task1),
        persistence.persist(task2)
      )))

      val tasks = await(persistence.getTasksToBeExecuted())
      tasks.size mustBe 1
      tasks.head mustBe task1
    }

    "get tasks that have existed for a while to be executed based on interval" in {
      val persistence = new InMemoryPersistence()

      val task1 = TaskInfo(
        taskClass = classOf[FirstTestingSchedulerClass],
        interval = 1 minute,
        initialDelay = 0 second,
        lastRun = Some(DateTime.now().minusMinutes(2))
      )
      val task2 = TaskInfo(
        taskClass = classOf[SecondTestingSchedulerClass],
        interval = 1 minute,
        lastRun = Some(DateTime.now())
      )

      await(Future.sequence(Seq(
        persistence.persist(task2),
        persistence.persist(task1)
      )))

      val tasks = await(persistence.getTasksToBeExecuted())

      tasks.size mustBe 1
      tasks.head mustBe task1
    }

    "not get tasks that are currently running" in {
      val persistence = new InMemoryPersistence()

      val task1 = TaskInfo(
        taskClass = classOf[FirstTestingSchedulerClass],
        interval = 1 minute,
        initialDelay = 0 second,
        lastRun = Some(DateTime.now().minusMinutes(2)),
        isRunning = true
      )

      await(persistence.persist(task1))

      val tasks = await(persistence.getTasksToBeExecuted())

      tasks.size mustBe 0
    }
  }
}
