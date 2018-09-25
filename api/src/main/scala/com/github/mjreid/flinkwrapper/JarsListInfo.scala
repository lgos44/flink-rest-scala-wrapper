package com.github.mjreid.flinkwrapper

import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
  * JobsList is the result of a call to GET /jobs. It contains IDs of the jobs grouped by their statuses.
  */
case class JarsListInfo(
                     address: String,
                     files: Seq[JarFileInfo]
                   )

object JarsListInfo {
  implicit val reads: Reads[JarsListInfo] = (
    (JsPath \ "address").read[String] and
      (JsPath \ "files").read[Seq[JarFileInfo]]
    )(JarsListInfo.apply _)
}
