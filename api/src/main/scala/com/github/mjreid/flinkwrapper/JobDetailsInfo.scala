package com.github.mjreid.flinkwrapper

import java.time.LocalDateTime

import com.github.mjreid.flinkwrapper.util.Readers
import play.api.libs.json._
import play.api.libs.functional.syntax._

import scala.concurrent.duration.Duration

case class JobDetailsInfo(
                id: String,
                name: String,
                isStoppable: Boolean,
                state: JobStatus.JobStatus,
                startTime: LocalDateTime,
                endTime: LocalDateTime,
                duration: Duration,
                now: LocalDateTime,
                stateTimestamps: StateTimes,
                vertices: Seq[JobVertexDetailsInfo],
                statusCounts: VertexTaskCounts,
                plan: JobPlan
)

object JobDetailsInfo {
  implicit val reads: Reads[JobDetailsInfo] = (
    (JsPath \ "jid").read[String] and
      (JsPath \ "name").read[String] and
      (JsPath \ "isStoppable").read[Boolean] and
      (JsPath \ "state").read[JobStatus.JobStatus] and
      (JsPath \ "start-time").read[LocalDateTime](Readers.millisLocalDateTimeReader) and
      (JsPath \ "end-time").read[LocalDateTime](Readers.millisLocalDateTimeReader) and
      (JsPath \ "duration").read[Duration](Readers.millisDurationReader) and
      (JsPath \ "now").read[LocalDateTime](Readers.millisLocalDateTimeReader) and
      (JsPath \ "timestamps").read[StateTimes] and
      (JsPath \ "vertices").read[Seq[JobVertexDetailsInfo]] and
      (JsPath \ "status-counts").read[VertexTaskCounts] and
      (JsPath \ "plan").read[JobPlan]
  )(JobDetailsInfo.apply _)
}