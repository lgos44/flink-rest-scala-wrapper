package com.github.mjreid.flinkwrapper

import play.api.libs.json.{JsPath, Reads}

case class JarPlanResponseBody(errors: Option[Seq[String]])

object JarPlanResponseBody {
  implicit val reads: Reads[JarPlanResponseBody] =
    (JsPath \ "errors").formatNullable[Seq[String]].map(JarPlanResponseBody.apply)
}
