package com.github.mjreid.flinkwrapper

import java.time.LocalDateTime

import com.github.mjreid.flinkwrapper.util.Readers
import play.api.libs.json.{JsPath, Reads}

import scala.concurrent.duration.Duration

case class TriggerResponse(requestId: String)


object TriggerResponse {
  implicit val reads: Reads[TriggerResponse] =
    (JsPath \ "request-id").read[String].map(TriggerResponse.apply)
}