package com.github.amazingdreams.play.scheduler.module

import javax.inject.{Inject, Provider}

import com.github.amazingdreams.play.scheduler.persistence.PlaySchedulerPersistence
import play.api.Environment
import play.api.inject.Injector

class PlaySchedulerPersistenceProvider @Inject()(playSchedulerConfiguration: PlaySchedulerConfiguration,
                                                 injector: Injector,
                                                 environment: Environment)
  extends Provider[PlaySchedulerPersistence] {

  val persistenceClass: Class[_ <: PlaySchedulerPersistence] =
    environment.classLoader.loadClass(playSchedulerConfiguration.persistenceClassName)
      .asSubclass(classOf[PlaySchedulerPersistence])

  override lazy val get: PlaySchedulerPersistence =
    injector.instanceOf(persistenceClass)
}
