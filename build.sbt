import java.nio.charset.StandardCharsets

import org.scalajs.linker.interface.{ESVersion, ModuleInitializer}

val javalibEntry = taskKey[File]("Path to rt.jar or \"jrt:/\"")
val copyCoreLibs = taskKey[Unit]("copy core libs")

val copyTreeSitterFiles = taskKey[Unit]("download and copy tree-sitter files")

inThisBuild(Def.settings(
  scalaVersion := "3.7.3",
  scalacOptions ++= Seq(
      "-deprecation",
      "-unchecked",
      "-feature",
      "-encoding", "utf8",
      "-Xcheck-macros",
  ),
  version := "0.1-SNAPSHOT",
))

/* Insert this in project scopes to work around
 * https://github.com/ScalablyTyped/Converter/issues/706
 */
val hackScalablyTypedRemoveSourceFuture: Seq[Setting[_]] = {
  import org.scalablytyped.converter.internal.scalajs.Versions
  import org.scalablytyped.converter.internal.ZincCompiler

  /* First, we need to override stConversionOptions.versions with a hacked
   * subclass that gets rid of "-source:future".
   */

  // Yes, I'm extending a case class; I will burn in hell.
  class HackedVersions(orig: Versions)
      extends Versions(orig.scala, orig.scalaJs) {
    override val scalacOptions: List[String] =
      orig.scalacOptions.filterNot(_ == "-source:future")

    override def toString(): String =
      s"${super.toString()} hacked with $scalacOptions"
  }

  val conversionSetting: Setting[_] = stConversionOptions := {
    val prev = stConversionOptions.value
    prev.copy(versions = new HackedVersions(prev.versions))
  }

  /* Unfortunately, the internal stInternalZincCompiler task recreates its
   * own Versions object, so we will also have to patch that one.
   * This is much trickier, because there is no real public access to that
   * thing. We're on the JVM, though, so nothing is ever *really* private,
   * if we try hard enough.
   */

  // Get access to the private `inputs` field of ZincCompiler:
  // https://github.com/ScalablyTyped/Converter/blob/02257bf3588da08deec5a3b07f306fcc6236642d/sbt-converter/src/main/scala/org/scalablytyped/converter/internal/ZincCompiler.scala#L25
  val inputsField = classOf[ZincCompiler].getDeclaredField("inputs")
  inputsField.setAccessible(true)

  // Access to a private setting
  // https://github.com/ScalablyTyped/Converter/blob/02257bf3588da08deec5a3b07f306fcc6236642d/sbt-converter/src/main/scala/org/scalablytyped/converter/plugin/ScalablyTypedConverterExternalNpmPlugin.scala#L15C19-L15C97
  val stInternalZincCompiler = taskKey[ZincCompiler]("Hijack compiler settings")

  val zincCompilerSetting: Setting[_] = stInternalZincCompiler := {
    val prev = stInternalZincCompiler.value
    val prevInputs = inputsField.get(prev).asInstanceOf[xsbti.compile.Inputs]
    val prevScalacOptions = prevInputs.options().scalacOptions()
    val newScalacOptions = prevScalacOptions.filterNot(_ == "-source:future")
    val newOptions = prevInputs.options().withScalacOptions(newScalacOptions)
    val newInputs = prevInputs.withOptions(newOptions)

    // And brutally set the immutable field, because we can.
    // (What do you mean, that's less bad than extending a case class?)
    inputsField.set(prev, newInputs)

    prev
  }

  Def.settings(
    conversionSetting,
    zincCompilerSetting
  )
}

val testSettings = Def.settings(
  libraryDependencies += "org.scalameta" %%% "munit" % "0.7.29" % Test,
)

lazy val root = project.in(file("."))
  .settings(
    name := "FunLabyrinthe",
  )
  .aggregate(
    compilerPlugin,
    core,
    coreInterface,
    coreBridge,
    mazes,
    html5Graphics,
    editorCommon,
    editorMain,
    editorRenderer,
  )

lazy val compilerPlugin = project.in(file("compiler-plugin"))
  .settings(
    name := "funlaby-compiler-plugin",
    libraryDependencies += "org.scala-lang" %% "scala3-compiler" % scalaVersion.value,
    exportJars := true,
  )

lazy val core = project
  .enablePlugins(ScalaJSPlugin)
  .dependsOn(compilerPlugin % "plugin")
  .settings(
    name := "funlaby-core",
    libraryDependencies += "org.portable-scala" %%% "portable-scala-reflect" % "1.1.2" cross CrossVersion.for3Use2_13,
    scalacOptions += "-experimental", // TODO Remove this when we upgrade from 3.7.3 to 3.8.0
    testSettings,
  )

lazy val coreInterface = project
  .in(file("core-interface"))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    name := "funlaby-core-interface",
    libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "2.4.0",
  )

lazy val coreBridge = project
  .in(file("core-bridge"))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    name := "funlaby-core-bridge",
    scalacOptions += "-experimental", // TODO Remove this when we upgrade from 3.7.3 to 3.8.0
    scalaJSLinkerConfig ~= {
      _.withModuleKind(ModuleKind.ESModule)
        .withExperimentalUseWebAssembly(true)
        .withESFeatures(_.withESVersion(ESVersion.ES2021))
    },

    // Patch the sjsir of JSPI to introduce primitives by hand
    Compile / compile := {
      val analysis = (Compile / compile).value

      val s = streams.value
      val classDir = (Compile / classDirectory).value
      val jspiIRFile = classDir / "com/funlabyrinthe/corebridge/JSPI$.sjsir"
      patchJSPIIR(jspiIRFile, s)

      analysis
    },

    // Patch __loader.js to work in Electron
    Compile / fastLinkJS := {
      val prev = (Compile / fastLinkJS).value
      val outputDir = (Compile / fastLinkJS / scalaJSLinkerOutputDirectory).value
      patchLoaderFileForElectron(outputDir)
      prev
    },
  )
  .dependsOn(coreInterface, core, mazes, html5Graphics)

lazy val mazes = project
  .enablePlugins(ScalaJSPlugin)
  .settings(
    name := "funlaby-mazes",
    scalacOptions += "-experimental", // TODO Remove this when we upgrade from 3.7.3 to 3.8.0
  )
  .dependsOn(core, compilerPlugin % "plugin")

lazy val html5Graphics = project.in(file("html5-graphics"))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    name := "funlaby-graphics-dom",
    libraryDependencies += "org.scala-js" %%% "scalajs-dom" % "2.4.0",
    scalacOptions += "-experimental", // TODO Remove this when we upgrade from 3.7.3 to 3.8.0
  )
  .dependsOn(core, compilerPlugin % "plugin")

lazy val editorCommon = project
  .in(file("editor-common"))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    name := "funlaby-editor-common",
  )

lazy val editorMain = project
  .in(file("editor-main"))
  .enablePlugins(ScalaJSPlugin, ScalablyTypedConverterExternalNpmPlugin)
  .settings(
    name := "funlaby-editor-main",
    libraryDependencies ++= Seq(
      "ch.epfl.scala" %%% "tasty-query" % "1.6.1",
      "org.scala-js" %%% "scalajs-linker" % scalaJSVersion cross CrossVersion.for3Use2_13,
    ),
    externalNpm := (LocalRootProject / baseDirectory).value,
    hackScalablyTypedRemoveSourceFuture,
    // electron does not support ES modules
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.CommonJSModule) },
    Compile / scalaJSModuleInitializers ++= Seq(
      ModuleInitializer.mainMethodWithArgs("com.funlabyrinthe.editor.main.Main", "main").withModuleID("main"),
    ),
    (Compile / fastLinkJS) := {
      (Compile / fastLinkJS)
        .dependsOn(copyCoreLibs)
        .dependsOn(editorRenderer / Compile / fastLinkJS)
        .dependsOn(coreBridge / Compile / fastLinkJS)
        .dependsOn(editorRenderer / copyTreeSitterFiles)
        .value
    },
    javalibEntry := {
      val s = streams.value
      val targetRTJar = target.value / "extracted-rt.jar"
      if (!targetRTJar.exists()) {
        s.log.info(s"Extracting jrt:/modules/java.base/ to $targetRTJar")
        extractRTJar(targetRTJar)
      }
      targetRTJar
    },
    copyCoreLibs := {
      val fullCp0 = Attributed.data((coreBridge / Compile / fullClasspathAsJars).value).toList
        .filter(!_.toString().contains("semanticdb"))
      val fullCp = javalibEntry.value :: fullCp0
      val targetDir = target.value / "libs"
      IO.createDirectory(targetDir)
      val pairs = fullCp.map(f => f -> targetDir / f.getName())
      IO.copy(pairs)

      // Also copy the compiler plugin
      val compilerPluginJar = (compilerPlugin / Compile / packageBin).value
      IO.copyFile(compilerPluginJar, target.value / "funlaby-compiler-plugin.jar")
    },
  )
  .dependsOn(editorCommon)

lazy val editorRenderer = project
  .in(file("editor-renderer"))
  .enablePlugins(ScalaJSPlugin, ScalablyTypedConverterExternalNpmPlugin)
  .settings(
    name := "funlaby-editor-renderer",
    scalaJSLinkerConfig ~= {
      _.withModuleKind(ModuleKind.ESModule)
        .withExperimentalUseWebAssembly(true)
        .withESFeatures(_.withESVersion(ESVersion.ES2021))
    },
    Compile / scalaJSModuleInitializers +=
      ModuleInitializer.mainMethodWithArgs("com.funlabyrinthe.editor.renderer.Renderer", "main").withModuleID("renderer"),
    libraryDependencies ++= Seq(
      "com.raquo" %%% "laminar" % "17.2.1",
      "be.doeraene" %%% "web-components-ui5" % "2.0.0",
      "com.lihaoyi" %%% "fansi" % "0.4.0",
    ),
    externalNpm := (LocalRootProject / baseDirectory).value,
    hackScalablyTypedRemoveSourceFuture,

    // Patch the sjsir of JSPI to introduce primitives by hand
    Compile / compile := {
      val analysis = (Compile / compile).value

      val s = streams.value
      val classDir = (Compile / classDirectory).value
      val jspiIRFile = classDir / "com/funlabyrinthe/editor/renderer/JSPI$.sjsir"
      patchJSPIIR(jspiIRFile, s)

      analysis
    },

    // Patch __loader.js to work in Electron
    Compile / fastLinkJS := {
      val prev = (Compile / fastLinkJS).value
      patchLoaderFileForElectron((Compile / fastLinkJS / scalaJSLinkerOutputDirectory).value)
      prev
    },

    copyTreeSitterFiles := {
      import scala.sys.process._

      val s = streams.value

      val targetDir = target.value / "tree-sitter-scala"
      IO.createDirectory(targetDir)

      val webTreeSitter = (LocalRootProject / baseDirectory).value / "node_modules/web-tree-sitter"

      var didAnything = false

      if (!(targetDir / "tree-sitter.js").exists) {
        s.log.info("Copying tree-sitter.js")
        IO.copyFile(webTreeSitter / "tree-sitter.js", targetDir / "tree-sitter.js")
        IO.append(targetDir / "tree-sitter.js", "\nexport default TreeSitter;\n")
        didAnything = true
      }

      if (!(targetDir / "tree-sitter.wasm").exists) {
        s.log.info("Copying tree-sitter.wasm")
        IO.copyFile(webTreeSitter / "tree-sitter.wasm", targetDir / "tree-sitter.wasm")
        didAnything = true
      }

      if (!(targetDir / "tree-sitter-scala.wasm").exists) {
        s.log.info("Downloading tree-sitter-scala.wasm")
        val treeSitterScalaURL =
          url("https://github.com/sjrd/tree-sitter-scala/releases/download/v0.20.2/tree-sitter-scala.wasm")
        (treeSitterScalaURL #> (targetDir / "tree-sitter-scala.wasm")).!
        didAnything = true
      }

      if (!(targetDir / "highlights.scm").exists) {
        s.log.info("Downloading highlights.scm")
        val highlightsURL =
          url("https://raw.githubusercontent.com/tree-sitter/tree-sitter-scala/v0.20.2/queries/scala/highlights.scm")
        (highlightsURL #> (targetDir / "highlights.scm")).!
        didAnything = true
      }

      if (didAnything)
        s.log.info("Done copying tree-sitter files")
    },
  )
  .dependsOn(coreInterface, editorCommon)

def patchJSPIIR(jspiIRFile: File, streams: TaskStreams): Unit = {
  import org.scalajs.ir.Names._
  import org.scalajs.ir.Trees._
  import org.scalajs.ir.Types._
  import org.scalajs.ir.WellKnownNames._

  val content = java.nio.ByteBuffer.wrap(java.nio.file.Files.readAllBytes(jspiIRFile.toPath()))
  val classDef = org.scalajs.ir.Serializers.deserialize(content)

  val newMethods = classDef.methods.mapConserve { m =>
    (m.methodName.simpleName.nameString, m.body) match {
      case ("async", Some(UnaryOp(UnaryOp.Throw, _))) =>
        implicit val pos = m.pos
        val closure = Closure(
          ClosureFlags.arrow.withAsync(true),
          m.args,
          Nil,
          None,
          AnyType,
          Apply(ApplyFlags.empty, m.args.head.ref,
              MethodIdent(MethodName("apply", Nil, ObjectRef)), Nil)(AnyType),
          m.args.map(_.ref)
        )
        val newBody = Some(JSFunctionApply(closure, Nil))
        m.copy(body = newBody)(m.optimizerHints, m.version)(m.pos)

      case ("await", Some(UnaryOp(UnaryOp.Throw, _))) =>
        implicit val pos = m.pos
        val newBody = Some(JSAwait(m.args.head.ref))
        m.copy(body = newBody)(m.optimizerHints, m.version)(m.pos)

      case _ =>
        m
    }
  }

  if (newMethods ne classDef.methods) {
    streams.log.info("Patching JSPI$.sjsir")
    val newClassDef = {
      import classDef._
      ClassDef(
        name,
        originalName,
        kind,
        jsClassCaptures,
        superClass,
        interfaces,
        jsSuperClass,
        jsNativeLoadSpec,
        fields,
        newMethods,
        jsConstructor,
        jsMethodProps,
        jsNativeMembers,
        topLevelExportDefs,
      )(optimizerHints)(pos)
    }
    val baos = new java.io.ByteArrayOutputStream()
    org.scalajs.ir.Serializers.serialize(baos, newClassDef)
    java.nio.file.Files.write(jspiIRFile.toPath(), baos.toByteArray())
  }
}

def patchLoaderFileForElectron(outputDir: File): Unit = {
  val loaderFile = outputDir / "__loader.js"
  val loaderContent = IO.readLines(loaderFile, StandardCharsets.UTF_8)
  val patchedLoaderContent = loaderContent.map {
    case "  if (resolvedURL.protocol === 'file:') {" => "  if (false) {"
    case other                                       => other
  }
  if (patchedLoaderContent != loaderContent)
    IO.writeLines(loaderFile, patchedLoaderContent, StandardCharsets.UTF_8)
}

def extractRTJar(targetRTJar: File): Unit = {
  import java.io.{IOException, FileOutputStream}
  import java.nio.file.{Files, FileSystems}
  import java.util.zip.{ZipEntry, ZipOutputStream}

  import scala.jdk.CollectionConverters._
  import scala.util.control.NonFatal

  val fs = FileSystems.getFileSystem(java.net.URI.create("jrt:/"))

  val zipStream = new ZipOutputStream(new FileOutputStream(targetRTJar))
  try {
    val javaBasePath = fs.getPath("modules", "java.base")
    Files.walk(javaBasePath).forEach({ p =>
      if (Files.isRegularFile(p)) {
        try {
          val data = Files.readAllBytes(p)
          val outPath = javaBasePath.relativize(p).iterator().asScala.mkString("/")
          val ze = new ZipEntry(outPath)
          zipStream.putNextEntry(ze)
          zipStream.write(data)
        } catch {
          case NonFatal(t) =>
            throw new IOException(s"Exception while extracting $p", t)
        }
      }
    })
  } finally {
    zipStream.close()
  }
}
