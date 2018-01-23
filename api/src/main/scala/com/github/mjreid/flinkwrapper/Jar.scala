package com.github.mjreid.flinkwrapper

import play.api.libs.functional.syntax._
import play.api.libs.json._

case class Entry(name: String, description: String)
case class Jar(
                id: String,
                name: String,
                uploaded: Long,
                entry: Seq[Entry]
              )

object Jar {
  implicit val readsEntry: Reads[Entry] = (
      (JsPath \ "name").read[String] and
      (JsPath \ "description").read[String]
    )(Entry.apply _)

  implicit val reads: Reads[Jar] = (
    (JsPath \ "id").read[String] and
      (JsPath \ "name").read[String] and
      (JsPath \ "uploaded").read[Long] and
      (JsPath \ "entry").read[Seq[Entry]]
    )(Jar.apply _)
}