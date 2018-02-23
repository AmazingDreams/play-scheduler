package tasks

import javax.inject.Inject

import com.github.amazingdreams.play.scheduler.tasks.SchedulerTask
import play.api.Logger

import scala.concurrent.{ExecutionContext, Future}

class LongRunningTask @Inject()()(implicit ec: ExecutionContext)
  extends SchedulerTask {

  val MINUTE = 60

  override def run(): Future[String] = Future {
    Logger.debug("Starting long running task")
    Thread.sleep(1000 * 5 * MINUTE)
    Logger.debug("Finished long running task")

    "OK"
  }
}
