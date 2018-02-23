package com.github.amazingdreams.play.scheduler.module

import com.github.amazingdreams.play.scheduler.tasks.SchedulerTask
import com.typesafe.config.ConfigFactory
import org.scalatestplus.play.PlaySpec
import play.api.Configuration

import scala.concurrent.duration._

class PlaySchedulerConfigurationSpec extends PlaySpec {
  "TaskConfiguration" should {
    "parse configuration values and return the initial list of tasks" in {
      val config = ConfigFactory.parseString(
        """
          |play.scheduler {
          |  prototype = {
          |    interval = 1 hour
          |    initialDelay = 0 seconds
          |    enabled = true
          |  }
          |
          |  tasks = [
          |    {
          |      task = com.github.amazingdreams.play.scheduler.tasks.SchedulerTask
          |      interval = 10 minutes
          |      initialDelay = 5 minutes
          |    }, {
          |      task = com.github.amazingdreams.play.scheduler.tasks.SchedulerTask
          |      interval = 1 hour
          |      initialDelay = 2 hours
          |      enabled = false
          |    }
          |  ]
          |}
        """.stripMargin)

      val schedulerConfiguration = new PlaySchedulerConfiguration(Configuration(config))

      val tasks = schedulerConfiguration.readTasks()
      tasks.size mustBe 2

      tasks(0).taskClass mustBe classOf[SchedulerTask]
      tasks(0).interval mustBe (10 minutes)
      tasks(0).initialDelay mustBe (5 minutes)
      tasks(0).isEnabled mustBe true

      tasks(1).taskClass mustBe classOf[SchedulerTask]
      tasks(1).interval mustBe (1 hour)
      tasks(1).initialDelay mustBe (2 hours)
      tasks(1).isEnabled mustBe false
    }
  }
}
