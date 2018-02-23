package com.github.amazingdreams.play.scheduler

import akka.actor.ActorSystem
import com.github.amazingdreams.play.scheduler.module.PlaySchedulerConfiguration
import com.github.amazingdreams.play.scheduler.persistence.InMemoryPersistence
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.inject.Injector

import scala.concurrent.ExecutionContext.Implicits.global

class PlaySchedulerSpec extends PlaySpec with MockitoSugar {

  trait TestSetup {
    lazy val actorSystem = mock[ActorSystem]
    lazy val injector = mock[Injector]
    lazy val configuration = mock[PlaySchedulerConfiguration]
    lazy val persistence = new InMemoryPersistence()

    lazy val scheduler = new PlayScheduler(
      actorSystem = actorSystem,
      injector = injector,
      configuration = configuration,
      persistence = persistence
    )
  }

  "PlayScheduler" should {
    "start scheduling" in new TestSetup {
      // Trigger construction
      val s = scheduler

    }
  }
}
