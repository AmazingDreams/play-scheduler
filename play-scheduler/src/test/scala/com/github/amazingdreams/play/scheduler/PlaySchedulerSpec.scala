package com.github.amazingdreams.play.scheduler

import akka.actor.ActorSystem
import com.github.amazingdreams.play.scheduler.PlayScheduler.Start
import com.github.amazingdreams.play.scheduler.module.PlaySchedulerConfiguration
import com.github.amazingdreams.play.scheduler.persistence.InMemoryPersistence
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.inject.Injector
import play.api.test.Injecting

import scala.concurrent.ExecutionContext.Implicits.global

class PlaySchedulerSpec extends PlaySpec
  with GuiceOneAppPerSuite
  with MockitoSugar
  with Injecting {

  trait TestSetup {
    val actorSystem = inject[ActorSystem]

    lazy val injector = mock[Injector]
    lazy val configuration = mock[PlaySchedulerConfiguration]
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
    "start scheduling" in new TestSetup {
      schedulerRef ! Start
    }
  }
}
