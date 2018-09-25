package com.github.mjreid.flinkwrapper

import play.api.libs.functional.syntax._
import play.api.libs.json._

case class SavepointTriggerRequestBody(
                                      targetDirectory: Option[String],
                                      cancelJob: Boolean
                                      )


object SavepointTriggerRequestBody {
  implicit val reads: Reads[SavepointTriggerRequestBody] = (
    (JsPath \ "target-directory").readNullable[String] and
      (JsPath \ "cancel-job").read[Boolean]
    ) (SavepointTriggerRequestBody.apply _)

  implicit val writes: Writes[SavepointTriggerRequestBody] = (
    (JsPath \ "target-directory").writeNullable[String] and
      (JsPath \ "cancel-job").write[Boolean]
  )(unlift(SavepointTriggerRequestBody.unapply))
}