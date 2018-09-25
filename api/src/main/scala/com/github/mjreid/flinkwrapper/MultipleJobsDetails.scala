package com.github.mjreid.flinkwrapper

import java.time.LocalDateTime

import com.github.mjreid.flinkwrapper.util.Readers
import play.api.libs.functional.syntax._
import play.api.libs.json._

import scala.concurrent.duration.Duration

/**
  * JobOverview contains a summary of all jobs, grouped by status.
  */
case class MultipleJobsDetails(
  running: Seq[JobSummary]
)

object MultipleJobsDetails {
  implicit val reads: Reads[MultipleJobsDetails] =
    (JsPath \ "jobs").read[Seq[JobSummary]].map(MultipleJobsDetails.apply)
}

case class JobSummary(
  jid: String,
  name: String,
  state: JobStatus.JobStatus,
  startTime: LocalDateTime,
  endTime: LocalDateTime,
  duration: Duration,
  lastModification: LocalDateTime,
  taskCounts: TaskCounts
)

object JobSummary {
  implicit val reads: Reads[JobSummary] = (
    (JsPath \ "jid").read[String] and
      (JsPath \ "name").read[String] and
      (JsPath \ "state").read[JobStatus.JobStatus] and
      (JsPath \ "start-time").read[LocalDateTime](Readers.millisLocalDateTimeReader) and
      (JsPath \ "end-time").read[LocalDateTime](Readers.millisLocalDateTimeReader) and
      (JsPath \ "duration").read[Duration](Readers.millisDurationReader) and
      (JsPath \ "last-modification").read[LocalDateTime](Readers.millisLocalDateTimeReader) and
      (JsPath \ "tasks").read[TaskCounts]
  )(JobSummary.apply _)
}

case class TaskCounts(
  total: Int,
  created: Int,
  scheduled: Int,
  deploying: Int,
  running: Int,
  finished: Int,
  canceling: Int,
  canceled: Int,
  failed: Int,
  reconciling: Int
)

object TaskCounts {
  implicit val reads: Reads[TaskCounts] = (
    (JsPath \ "total").read[Int] and
      (JsPath \ "created").read[Int] and
      (JsPath \ "scheduled").read[Int] and
      (JsPath \ "deploying").read[Int] and
      (JsPath \ "running").read[Int] and
      (JsPath \ "finished").read[Int] and
      (JsPath \ "canceling").read[Int] and
      (JsPath \ "canceled").read[Int] and
      (JsPath \ "failed").read[Int] and
      (JsPath \ "reconciling").read[Int]
    ) (TaskCounts.apply _)
}

object ExecutionStatus {
  /**
    * ExecutionStatus represents all possible runtime statuses for tasks. See ExecutionState.java in the Flink source.
    */
  sealed trait ExecutionStatus
  case object Created extends ExecutionStatus
  case object Scheduled extends ExecutionStatus
  case object Deploying extends ExecutionStatus
  case object Running extends ExecutionStatus
  case object Finished extends ExecutionStatus
  case object Canceling extends ExecutionStatus
  case object Canceled extends ExecutionStatus
  case object Failed extends ExecutionStatus
  case object Reconciling extends ExecutionStatus

  implicit val reads: Reads[ExecutionStatus.ExecutionStatus] = Reads {
    case JsString(statusString) =>
      statusString.toUpperCase match {
        case "CREATED" => JsSuccess(ExecutionStatus.Created)
        case "SCHEDULED" => JsSuccess(ExecutionStatus.Scheduled)
        case "DEPLOYING" => JsSuccess(ExecutionStatus.Deploying)
        case "RUNNING" => JsSuccess(ExecutionStatus.Running)
        case "FINISHED" => JsSuccess(ExecutionStatus.Finished)
        case "CANCELING" => JsSuccess(ExecutionStatus.Canceling)
        case "CANCELED" => JsSuccess(ExecutionStatus.Canceled)
        case "FAILED" => JsSuccess(ExecutionStatus.Failed)
        case "RECONCILING" => JsSuccess(ExecutionStatus.Reconciling)
        case _ => JsError("Not a valid execution status")
      }
    case _ => JsError("Not a valid execution status")
  }
}


object JobStatus {

  /**
    * JobStatus represents the possible job states once it has been accepted by the Flink job manager. See Flink source,
    * JobStatus.java
    */
  sealed trait JobStatus

  case object Created extends JobStatus

  case object Running extends JobStatus

  case object Finished extends JobStatus

  case object Failing extends JobStatus

  case object Canceled extends JobStatus

  case object Failed extends JobStatus

  case object Cancelling extends JobStatus

  case object Suspended extends JobStatus

  case object Suspending extends JobStatus

  case object Restarting extends JobStatus

  case object Reconciling extends JobStatus

  implicit val reads: Reads[JobStatus.JobStatus] = Reads {
    case JsString(statusString) =>
      statusString.toUpperCase match {
        case "CREATED" => JsSuccess(JobStatus.Created)
        case "RUNNING" => JsSuccess(JobStatus.Running)
        case "FAILING" => JsSuccess(JobStatus.Failing)
        case "FAILED" => JsSuccess(JobStatus.Failed)
        case "CANCELLING" => JsSuccess(JobStatus.Cancelling)
        case "CANCELED" => JsSuccess(JobStatus.Canceled)
        case "FINISHED" => JsSuccess(JobStatus.Finished)
        case "RESTARTING" => JsSuccess(JobStatus.Restarting)
        case "SUSPENDING" => JsSuccess(JobStatus.Suspending)
        case "SUSPENDED" => JsSuccess(JobStatus.Suspended)
        case "RECONCILING" => JsSuccess(JobStatus.Reconciling)
        case _ => JsError("Not a valid job status")
      }
    case _ => JsError("Not a valid job status")
  }
}