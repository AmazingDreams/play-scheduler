package com.github.amazingdreams.play.scheduler.module

import com.github.amazingdreams.play.scheduler.PlayScheduler
import com.github.amazingdreams.play.scheduler.persistence.{InMemoryPersistence, PlaySchedulerPersistence}
import com.typesafe.config.ConfigFactory
import org.scalatestplus.play.PlaySpec
import play.api.Configuration
import play.api.inject.guice.GuiceApplicationBuilder

import scala.concurrent.ExecutionContext

class PlaySchedulerModuleSpec extends PlaySpec {

  trait IntegrationTestSetup {
    lazy val configString = """"""
    lazy val config = ConfigFactory.parseString(configString)

    lazy val app = new GuiceApplicationBuilder()
      .configure(Configuration(config))
      .bindings(new PlaySchedulerModule())
      .build()
  }

  "PlaySchedulerModule" should {
    "load properly" in new IntegrationTestSetup {
      implicit val ec = app.injector.inject[ExecutionContext]

      val scheduler = app.actorSystem.actorSelection(s"user/${PlayScheduler.ACTOR_NAME}")
      val persistence = app.injector.instanceOf[PlaySchedulerPersistence]

      scheduler must not be null
      persistence must not be null

      persistence.getClass mustBe classOf[InMemoryPersistence]
    }
  }
}
