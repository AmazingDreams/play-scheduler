package com.github.amazingdreams.play.scheduler.module

import akka.actor.{ActorSystem, Props}
import com.github.amazingdreams.play.scheduler.persistence.PlaySchedulerPersistence
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers._
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.inject.Injector

import scala.concurrent.ExecutionContext.Implicits.global

class PlaySchedulerStarterSpec extends PlaySpec with MockitoSugar {

  trait TestSetup {
    lazy val configuration = mock[PlaySchedulerConfiguration]
    lazy val injector = mock[Injector]
    lazy val persistence = mock[PlaySchedulerPersistence]
    lazy val actorSystem = mock[ActorSystem]

    def createStarter() = new PlaySchedulerStarter(
      system = actorSystem,
      configuration = configuration,
      injector = injector,
      persistence = persistence
    )
  }

  "PlayScheduler" should {
    "not do anything when module is disabled" in new TestSetup {
      when(configuration.isEnabled).thenReturn(false)

      createStarter()

      verify(actorSystem, never).actorOf(any[Props], any[String])
    }

    "start the play scheduler in a local environment properly" in new TestSetup {
      when(configuration.isEnabled).thenReturn(true)
      when(configuration.useAkkaClustering).thenReturn(false)

      createStarter()

      verify(actorSystem, times(1)).actorOf(any[Props], any[String])
    }

    "start the play scheduler through a cluster proxy" in new TestSetup {
      when(configuration.isEnabled).thenReturn(true)
      when(configuration.useAkkaClustering).thenReturn(true)

      // Null pointer is expected as the ActorSystem is mocked
      intercept[NullPointerException] {
        createStarter()
      }
    }
  }

}
