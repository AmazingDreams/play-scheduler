package com.github.amazingdreams.play.scheduler.persistence

import javax.inject.{Inject, Provider}

import com.github.amazingdreams.play.scheduler.module.PlaySchedulerConfiguration
import play.api.inject.Injector

class PlaySchedulerPersistenceProvider @Inject()(playSchedulerConfiguration: PlaySchedulerConfiguration,
                                                 injector: Injector)
  extends Provider[PlaySchedulerPersistence] {

  override lazy val get: PlaySchedulerPersistence =
    injector.instanceOf(playSchedulerConfiguration.persistenceClass)
}
