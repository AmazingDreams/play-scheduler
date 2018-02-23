package com.github.amazingdreams.play.scheduler.module

import com.github.amazingdreams.play.scheduler.persistence.PlaySchedulerPersistence
import com.github.amazingdreams.play.scheduler.PlayScheduler
import play.api.inject.{Binding, Module}
import play.api.{Configuration, Environment}

class PlaySchedulerModule extends Module {

  override def bindings(environment: Environment,
                        configuration: Configuration): Seq[Binding[_]] = Seq(
    bind[PlaySchedulerPersistence].toProvider[PlaySchedulerPersistenceProvider],
    bind[PlayScheduler].toSelf.eagerly()
  )
}
