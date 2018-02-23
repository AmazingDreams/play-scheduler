package tasks

import javax.inject.Inject

import com.github.amazingdreams.play.scheduler.tasks.SchedulerTask
import play.api.Logger

import scala.concurrent.{ExecutionContext, Future}

class ShortRunningTask @Inject()()(implicit ec: ExecutionContext)
  extends SchedulerTask {

  override def run(): Future[String] = Future {
    Logger.debug("Starting short running task")
    Thread.sleep(1000 * 5)
    Logger.debug("Finished short running task")

    "OK"
  }
}
