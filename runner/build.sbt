unmanagedJars in Compile += Attributed.blank(
    file(scala.util.Properties.javaHome) / "lib" / "jfxrt.jar")

fork in run := true

libraryDependencies ++= Seq(
    "org.scalafx" %% "scalafx" % "1.0.0-M2"
)
