package com.github.mjreid.flinkwrapper

import play.api.libs.functional.syntax._
import play.api.libs.json._

case class JarEntryInfo(name: String, description: Option[String])


case class JarFileInfo(
                id: String,
                name: String,
                uploaded: Long,
                entry: Seq[JarEntryInfo]
              )

object JarFileInfo {
  implicit val readsEntry: Reads[JarEntryInfo] = (
      (JsPath \ "name").read[String] and
      (JsPath \ "description").readNullable[String]
    )(JarEntryInfo.apply _)

  implicit val reads: Reads[JarFileInfo] = (
    (JsPath \ "id").read[String] and
      (JsPath \ "name").read[String] and
      (JsPath \ "uploaded").read[Long] and
      (JsPath \ "entry").read[Seq[JarEntryInfo]]
    )(JarFileInfo.apply _)
}