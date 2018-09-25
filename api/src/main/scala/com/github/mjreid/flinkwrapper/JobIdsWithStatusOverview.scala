package com.github.mjreid.flinkwrapper

import com.github.mjreid.flinkwrapper.JobStatus.JobStatus
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * JobsList is the result of a call to GET /jobs. It contains IDs of the jobs grouped by their statuses.
  */
case class JobIdsWithStatusOverview(
  jobs: Seq[JobIdWithStatus]
)

case class JobIdWithStatus(id: String, status: JobStatus.JobStatus)

object JobIdWithStatus {
  implicit val reads: Reads[JobIdWithStatus] = (
    (JsPath \ "id").read[String] and
    (JsPath \ "status").read[JobStatus.JobStatus]
    )(JobIdWithStatus.apply _)
}

object JobIdsWithStatusOverview {
  implicit val reads: Reads[JobIdsWithStatusOverview] =
      (JsPath \ "jobs").read[Seq[JobIdWithStatus]].map(JobIdsWithStatusOverview.apply)
}
