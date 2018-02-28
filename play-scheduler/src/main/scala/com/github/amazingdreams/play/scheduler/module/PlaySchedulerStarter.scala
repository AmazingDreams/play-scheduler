package com.github.amazingdreams.play.scheduler.module

import javax.inject.Inject

import akka.actor.ActorSystem
import akka.cluster.singleton.{ClusterSingletonManager, ClusterSingletonManagerSettings, ClusterSingletonProxy, ClusterSingletonProxySettings}
import com.github.amazingdreams.play.scheduler.PlayScheduler
import com.github.amazingdreams.play.scheduler.PlayScheduler.Stop
import com.github.amazingdreams.play.scheduler.persistence.PlaySchedulerPersistence
import play.api.Logger
import play.api.inject.Injector

import scala.concurrent.ExecutionContext

class PlaySchedulerStarter @Inject()(system: ActorSystem,
                                     injector: Injector,
                                     configuration: PlaySchedulerConfiguration,
                                     persistence: PlaySchedulerPersistence)
                                    (implicit ec: ExecutionContext) {

  val logger = Logger(getClass)

  if (configuration.isEnabled) {
    if (configuration.useAkkaClustering) {
      startUsingAkkaCluster()
    } else {
      startLocal()
    }
  }

  private def startUsingAkkaCluster(): Unit = {
    system.actorOf(
      ClusterSingletonManager.props(
        singletonProps = PlayScheduler.Props(
          injector = injector,
          configuration = configuration,
          persistence = persistence
        ),
        terminationMessage = Stop,
        settings = ClusterSingletonManagerSettings(system)
          .withSingletonName(PlayScheduler.ACTOR_NAME)
      ),
      PlayScheduler.ACTOR_NAME
    )

    system.actorOf(
      ClusterSingletonProxy.props(
        singletonManagerPath = s"/user/${PlayScheduler.ACTOR_NAME}",
        settings = ClusterSingletonProxySettings(system)
      ),
      name = PlayScheduler.PROXY_NAME
    )
  }

  private def startLocal(): Unit = {
    logger.info("Starting scheduler locally")

    system.actorOf(PlayScheduler.Props(
      injector = injector,
      configuration = configuration,
      persistence = persistence
    ), PlayScheduler.ACTOR_NAME)
  }
}
