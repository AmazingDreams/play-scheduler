package tasks.persistence

import java.io.{File, PrintWriter}
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}

import com.github.amazingdreams.play.scheduler.persistence.InMemoryPersistence
import com.github.amazingdreams.play.scheduler.tasks.{SchedulerTask, TaskInfo}
import play.api.libs.json.JodaReads._
import play.api.libs.json.JodaWrites._
import play.api.libs.json._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.io.Source

object MyCustomPersistence {
  implicit val classReads: Reads[Class[_ <: SchedulerTask]] = (json: JsValue) => JsSuccess(Class.forName(json.as[String]).asSubclass(classOf[SchedulerTask]))
  implicit val classWrites: Writes[Class[_ <: SchedulerTask]] = (o: Class[_ <: SchedulerTask]) => JsString(o.getName)

  implicit val durationReads: Reads[FiniteDuration] = (json: JsValue) => JsSuccess(FiniteDuration(json.as[Long], TimeUnit.SECONDS))
  implicit val durationWrites: Writes[FiniteDuration] = (o: FiniteDuration) => JsNumber(o.toSeconds)

  implicit val taskReader = Json.reads[TaskInfo]
  implicit val taskWriter = Json.writes[TaskInfo]
}

/**
  * This class syncs the tasks with a file
  *
  * It uses the InMemoryPersistence as a backend
  */
@Singleton
class MyCustomPersistence @Inject()()(implicit ec: ExecutionContext)
  extends InMemoryPersistence {

  import MyCustomPersistence._

  val file = new File("/tmp/my-custom-persistence.txt")

  readTasksFromFile()

  def readTasksFromFile() = {
    if (file.exists()) {
      val tasks = Source.fromFile(file).getLines().map { line =>
        Json.fromJson[TaskInfo](Json.parse(line)).asOpt.get
      }

      Await.result(Future.sequence(tasks.map(super.persist)), 3 seconds)
    }
  }

  def writeTasksToFile() = {
    super.getTasks.map { tasks =>
      file.createNewFile()

      val writer = new PrintWriter(file)
      tasks.foreach { task =>
        writer.write(Json.stringify(Json.toJson(task)) + "\n")
      }
      writer.close()
    }
  }

  override def persist(taskInfo: TaskInfo): Future[TaskInfo] =
    super.persist(taskInfo).map { updatedTaskInfo =>
      writeTasksToFile()

      updatedTaskInfo
    }
}
