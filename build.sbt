import sbt._

enablePlugins(ScalazPlugin)

organization in ThisBuild := "org.scalaz"

version in ThisBuild := "0.1-SNAPSHOT"

publishTo in ThisBuild := {
        val nexus = "https://oss.sonatype.org/"
        if(isSnapshot.value)
            Some("snapshots" at nexus + "content/repositories/snapshots")
        else
            Some("releases" at nexus + "service/local/staging/deploy/maven2")
    }

dynverSonatypeSnapshots in ThisBuild := true

lazy val sonataCredentials = for {
    username <- sys.env.get("SONATYPE_USERNAME")
    password <- sys.env.get("SONATYPE_PASSWORD")
} yield Credentials("Sonatype Nexus Repository Manager", "oss.sonatype.org", username, password)

credentials in ThisBuild ++= sonataCredentials.toSeq

lazy val scalazVersion = "7.2.25"
lazy val scalaTest = "3.0.5"
lazy val scalaCheck = "1.14.0"

lazy val standardSettings = Seq(
    wartremoverWarnings ++= Warts.all,
    libraryDependencies ++= Seq(
        "org.scalaz" %% "scalaz-core" % scalazVersion % "compile, test",
        "org.scalatest" %% "scalatest" % scalaTest % "test",
        "org.scalacheck" %% "scalacheck" % scalaCheck % "test"
    ),
    resolvers += Resolver.sonatypeRepo("releases")
)

lazy val root = Project("root", file("."))
    .settings(name := "scalaz-nio")
    .settings(standardSettings)