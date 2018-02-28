package controllers

import javax.inject._

import com.github.amazingdreams.play.scheduler.PlaySchedulerManager
import play.api.mvc._

import scala.concurrent.ExecutionContext

@Singleton
class HomeController @Inject()(cc: ControllerComponents,
                               playSchedulerManager: PlaySchedulerManager)
                              (implicit ec: ExecutionContext)
  extends AbstractController(cc) {

  def index() = Action.async { implicit request: Request[AnyContent] =>
    playSchedulerManager.getTasks().map { tasks =>
      Ok(views.html.index(tasks))
    }
  }
}
