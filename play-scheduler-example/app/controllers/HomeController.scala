package controllers

import javax.inject._

import com.github.amazingdreams.play.scheduler.PlayScheduler
import play.api.mvc._

import scala.concurrent.ExecutionContext

@Singleton
class HomeController @Inject()(cc: ControllerComponents,
                               playScheduler: PlayScheduler)
                              (implicit ec: ExecutionContext)
  extends AbstractController(cc) {

  def index() = Action.async { implicit request: Request[AnyContent] =>
    playScheduler.getTasks().map { tasks =>
      Ok(views.html.index(tasks))
    }
  }
}
