package com.github.amazingdreams.play.scheduler

import akka.actor.ActorSystem
import com.github.amazingdreams.play.scheduler.PlayScheduler.{RunTask, Start, Stop}
import com.github.amazingdreams.play.scheduler.module.PlaySchedulerConfiguration
import com.github.amazingdreams.play.scheduler.persistence.InMemoryPersistence
import com.github.amazingdreams.play.scheduler.tasks.{SchedulerTask, TaskInfo}
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.test.Helpers._
import play.api.test.Injecting

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

class TestingSchedulerTask extends SchedulerTask {
  override def run(): Future[String] = Future("OK")
}
class BrokenTestingSchedulerTask extends SchedulerTask {
  override def run(): Future[String] = throw new NullPointerException
}

class PlaySchedulerSpec extends PlaySpec
  with GuiceOneAppPerSuite
  with MockitoSugar
  with Injecting {

  trait TestSetup {
    lazy val actorSystem = inject[ActorSystem]
    lazy val injector = app.injector

    lazy val configuration = mock[PlaySchedulerConfiguration]
    when(configuration.readTasks()).thenReturn(Seq.empty)

    lazy val persistence = new InMemoryPersistence()

    lazy val schedulerRef = actorSystem.actorOf(
      PlayScheduler.Props(
        injector = injector,
        configuration = configuration,
        persistence = persistence
      )
    )
  }

  "PlayScheduler" should {
    "start and stop scheduling" in new TestSetup {
      schedulerRef ! Start

      Thread.sleep(1000)

      schedulerRef ! Stop
    }

    "run a task" in new TestSetup {
      schedulerRef ! RunTask(TaskInfo(
        taskClass = classOf[TestingSchedulerTask],
        interval = 10 seconds
      ))

      Thread.sleep(1000)

      await(persistence.getTasks).size mustBe 1
    }

    "run a task that fails" in new TestSetup {
      schedulerRef ! RunTask(TaskInfo(
        taskClass = classOf[BrokenTestingSchedulerTask],
        interval = 10 seconds
      ))

      Thread.sleep(1000)

      val tasks = await(persistence.getTasks)
      tasks.size mustBe 1

      tasks(0).isEnabled mustBe false
    }
  }
}
