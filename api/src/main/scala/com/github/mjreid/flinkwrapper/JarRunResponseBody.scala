package com.github.mjreid.flinkwrapper

import play.api.libs.json._

case class JarRunResponseBody(
  jobId: String
)

object JarRunResponseBody {
  implicit val reads: Reads[JarRunResponseBody] =
    (JsPath \ "jobid").read[String].map(JarRunResponseBody.apply)
}
