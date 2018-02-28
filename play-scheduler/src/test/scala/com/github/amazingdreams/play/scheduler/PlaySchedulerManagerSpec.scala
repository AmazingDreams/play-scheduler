package com.github.amazingdreams.play.scheduler

import com.github.amazingdreams.play.scheduler.persistence.PlaySchedulerPersistence
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.test.Helpers._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PlaySchedulerManagerSpec extends PlaySpec with MockitoSugar {

  trait TestSetup {
    lazy val persistence = mock[PlaySchedulerPersistence]

    lazy val manager = new PlaySchedulerManager(
      playSchedulerPersistence = persistence
    )
  }

  "PlaySchedulerManager" should {
    "get the tasks" in new TestSetup {
      when(persistence.getTasks()).thenReturn(Future(Seq.empty))

      val result = await(manager.getTasks())
      result.size mustBe 0

      verify(persistence, times(1)).getTasks()
    }
  }
}
