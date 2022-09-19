ThisBuild / tlBaseVersion := "0.0"

ThisBuild / organization := "com.armanbilge"
ThisBuild / organizationName := "Arman Bilge"
ThisBuild / developers += tlGitHubDev("armanbilge", "Arman Bilge")
ThisBuild / startYear := Some(2022)
ThisBuild / tlSonatypeUseLegacyHost := false

ThisBuild / crossScalaVersions := Seq("3.1.3", "2.13.8")

ThisBuild / githubWorkflowBuildMatrixFailFast := Some(false)
ThisBuild / githubWorkflowJavaVersions := Seq(JavaSpec.temurin("17"))
ThisBuild / githubWorkflowOSes := Seq("ubuntu-20.04", "ubuntu-22.04")

ThisBuild / githubWorkflowBuildPreamble +=
  WorkflowStep.Run(
    List("brew update", "brew install liburing"),
    name = Some("Install liburing")
  )

val fs2Version = "3.3.0"
val munitCEVersion = "2.0.0-M3"

ThisBuild / nativeConfig ~= { c =>
  c.withLinkingOptions(c.linkingOptions :+ "-L/home/linuxbrew/.linuxbrew/lib")
}
ThisBuild / envVars ++= Map("LD_LIBRARY_PATH" -> "/home/linuxbrew/.linuxbrew/lib")

lazy val root = tlCrossRootProject.aggregate(uring)

lazy val uring = project
  .in(file("uring"))
  .enablePlugins(ScalaNativePlugin)
  .settings(
    name := "fs2-io_uring",
    libraryDependencies ++= Seq(
      "co.fs2" %%% "fs2-io" % fs2Version,
      "org.typelevel" %%% "munit-cats-effect" % munitCEVersion
    )
  )
