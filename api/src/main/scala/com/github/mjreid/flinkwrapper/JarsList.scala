package com.github.mjreid.flinkwrapper

import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
  * JobsList is the result of a call to GET /jobs. It contains IDs of the jobs grouped by their statuses.
  */
case class JarsList(
                     address: String,
                     files: Seq[Jar]
                   )

object JarsList {
  implicit val reads: Reads[JarsList] = (
    (JsPath \ "address").read[String] and
      (JsPath \ "files").read[Seq[Jar]]
    )(JarsList.apply _)
}
