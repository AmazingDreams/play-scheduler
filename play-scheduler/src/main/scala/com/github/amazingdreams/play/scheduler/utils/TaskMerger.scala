package com.github.amazingdreams.play.scheduler.utils

import com.github.amazingdreams.play.scheduler.tasks.TaskInfo

object TaskMerger {

  def merge(configured: Seq[TaskInfo], stored: Seq[TaskInfo]): Seq[TaskInfo] = {
    val storedTaskClasses = stored.map(_.taskClass)

    // Split configured tasks based on new & existing
    val (existingTasks, newTasks) = configured.partition { configuredTask =>
      storedTaskClasses.contains(configuredTask.taskClass)
    }

    // Merge and/or disable tasks that are stored
    val combined = stored.map { storedTask =>
      existingTasks.find(_.taskClass == storedTask.taskClass) match {
        case Some(existingTask) =>
          storedTask.copy(
            interval = existingTask.interval,
            isEnabled = existingTask.isEnabled
          )
        case None =>
          storedTask.copy(
            isEnabled = false
          )
      }
    }

    // Return combine & new tasks
    combined ++ newTasks
  }
}
