import Scalaz._

name := "scalaz-nio"

inThisBuild(
  List(
    organization := "org.scalaz",
    homepage := Some(url("https://scalaz.github.io/scalaz-nio/")),
    licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
    developers := List(
      Developer(
        "ysusuk",
        "Iurii Susuk",
        "iuriisusuk@gmail.com",
        url("http://iuriisusuk.com")
      )
    )
  )
)

addCommandAlias("fmt", "all scalafmtSbt scalafmt test:scalafmt")
addCommandAlias("check", "all scalafmtSbtCheck scalafmtCheck test:scalafmtCheck")

lazy val root =
  (project in file("."))
    .settings(
      stdSettings("nio")
    )
    .enablePlugins(TutPlugin)

resolvers +=
  "Sonatype OSS Snapshots".at("https://oss.sonatype.org/content/repositories/snapshots")
