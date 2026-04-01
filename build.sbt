val scala3Version = "3.4.2"

ThisBuild / scalaVersion := scala3Version
ThisBuild / organization := "io.lukso"
ThisBuild / name         := "scala-lukso"
ThisBuild / version      := "0.1.0-SNAPSHOT"

ThisBuild / homepage := Some(url("https://github.com/anthropic/scala-lukso"))
ThisBuild / licenses := List("Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0"))
ThisBuild / scmInfo := Some(
  ScmInfo(url("https://github.com/anthropic/scala-lukso"), "scm:git@github.com:anthropic/scala-lukso.git")
)

// Maven Central publishing
ThisBuild / publishMavenStyle := true

val zioVersion     = "2.1.14"
val zioHttpVersion = "3.0.1"
val zioJsonVersion = "0.7.3"

lazy val root = (project in file("."))
  .settings(
    name := "scala-lukso",
    libraryDependencies ++= Seq(
      // Core
      "dev.zio"              %% "zio"               % zioVersion,
      "dev.zio"              %% "zio-streams"        % zioVersion,
      "dev.zio"              %% "zio-http"           % zioHttpVersion,
      "dev.zio"              %% "zio-json"           % zioJsonVersion,
      // Crypto
      "org.bouncycastle"      % "bcprov-jdk18on"    % "1.78.1",
      // ABI
      "org.web3j"             % "abi"               % "4.12.3",
      // Test
      "dev.zio"              %% "zio-test"           % zioVersion % Test,
      "dev.zio"              %% "zio-test-sbt"       % zioVersion % Test,
      "dev.zio"              %% "zio-test-magnolia"  % zioVersion % Test,
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )
