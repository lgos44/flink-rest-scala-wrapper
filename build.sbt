import sbt.url

name := "flink-rest-scala-wrapper"

lazy val commonSettings = Seq(
  organization := "com.github.mjreid",
  version := "0.0.8",
  scalaVersion := "2.11.11"
)

lazy val root = project.in(file("."))
  .settings(commonSettings)
  .settings(publish := { })
  .settings(publishArtifact := false)
  .aggregate(api, sampleApp)


lazy val api = project.in(file("api"))
  .settings(commonSettings)
  .settings(name := "flink-wrapper")
  .settings(libraryDependencies ++= Dependencies.all)
  .settings(Seq(
    licenses := Seq("APL2" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt")),

    sonatypeProfileName := "com.github.lgos44",

    publishMavenStyle := true,

    homepage := Some(url("https://github.com/lgos44/flink-rest-scala-wrapper")),

    scmInfo := Some(
      ScmInfo(
        url("https://github.com/lgos44/flink-rest-scala-wrapper.git"),
        "scm:git@github.com:lgos44/flink-rest-scala-wrapper.git"
      )
    ),

    developers := List(
      Developer(id="mjreid", name="Michael Reid", email="reidmichaeljames@gmail.com", url=url("https://github.com/mjreid/")),
      Developer(id="lgos44", name="Luis Gustavo", email="lgos@poli.ufrj.br", url=url("https://github.com/lgos44/"))
    ))
  )

lazy val sampleApp = project.in(file("sample-app"))
  .settings(commonSettings)
  .settings(name := "sample-app")
  .settings(publish := { })
  .settings(publishArtifact := false)
  .dependsOn(api)


// Sonatype/maven publishing stuff

useGpg := true

