package com.github.amazingdreams.play.scheduler.module

import javax.inject.{Inject, Provider}

import com.github.amazingdreams.play.scheduler.persistence.PlaySchedulerPersistence
import play.api.inject.Injector

class PlaySchedulerPersistenceProvider @Inject()(playSchedulerConfiguration: PlaySchedulerConfiguration,
                                                 injector: Injector)
  extends Provider[PlaySchedulerPersistence] {

  val persistenceClass: Class[_ <: PlaySchedulerPersistence] =
    Class.forName(playSchedulerConfiguration.persistenceClassName)
      .asSubclass(classOf[PlaySchedulerPersistence])

  override lazy val get: PlaySchedulerPersistence =
    injector.instanceOf(persistenceClass)
}
