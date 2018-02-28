package com.github.amazingdreams.play.scheduler.module

import javax.inject.{Inject, Singleton}

import com.github.amazingdreams.play.scheduler.persistence.InMemoryPersistence
import com.github.amazingdreams.play.scheduler.tasks.{SchedulerTask, TaskInfo}
import play.api.{Configuration, Environment}

import scala.concurrent.duration._

object PlaySchedulerConfiguration {
  val BASE_CONFIG_PATH = "play.scheduler"
}

@Singleton
class PlaySchedulerConfiguration @Inject()(configuration: Configuration,
                                           environment: Environment) {
  import PlaySchedulerConfiguration._

  def isEnabled: Boolean =
    configuration.getOptional[Boolean](s"$BASE_CONFIG_PATH.enabled")
      .getOrElse(true)

  def useAkkaClustering: Boolean =
    configuration.getOptional[Boolean](s"$BASE_CONFIG_PATH.cluster")
      .getOrElse(false)

  def schedulerInterval: FiniteDuration =
    configuration.getOptional[FiniteDuration](s"$BASE_CONFIG_PATH.interval")
      .getOrElse(10 seconds)

  def persistenceClassName: String =
    configuration.getOptional[String](s"$BASE_CONFIG_PATH.persistence")
      .getOrElse {
        classOf[InMemoryPersistence].getCanonicalName
      }

  def readTasks(): Seq[TaskInfo] =
    configurationWithTaskPrototype
      .getPrototypedSeq(s"$BASE_CONFIG_PATH.tasks", s"$BASE_CONFIG_PATH.prototype")
      .map { configEntry =>
        val clazz = environment.classLoader
          .loadClass(configEntry.get[String]("task"))
          .asSubclass(classOf[SchedulerTask])

        TaskInfo(
          taskClass = clazz,
          interval = configEntry.get[FiniteDuration]("interval"),
          initialDelay = configEntry.get[FiniteDuration]("initialDelay"),
          isEnabled = configEntry.get[Boolean]("enabled")
        )
      }

  def configurationWithTaskPrototype: Configuration =
    configuration ++ Configuration(
      s"$BASE_CONFIG_PATH.prototype.interval" -> "1 hour",
      s"$BASE_CONFIG_PATH.prototype.initialDelay" -> "0 seconds",
      s"$BASE_CONFIG_PATH.prototype.enabled" -> "true"
    )
}
