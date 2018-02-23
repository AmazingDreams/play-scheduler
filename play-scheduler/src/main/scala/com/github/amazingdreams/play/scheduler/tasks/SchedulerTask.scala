package com.github.amazingdreams.play.scheduler.tasks

import scala.concurrent.Future

trait SchedulerTask {
  def run(): Future[String]
}
