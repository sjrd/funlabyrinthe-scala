package com.funlabyrinthe.editor.renderer.codemirror

final case class Problem(
  severity: Problem.Severity,
  sourceName: String,
  line: Int,
  startColumn: Int,
  endColumn: Int,
  message: String,
)

object Problem:
  enum Severity:
    case Info, Warning, Error

  // example: [error] .\MursEtMurets.scala:29:17
  private val headerLine = raw"""\[([a-z]+)\] (.+):(\d+):(\d+)""".r

  def parseFromLogs(logLines: List[String]): List[Problem] =
    logLines match
      case headerLine(severityStr, path, startLineStr, startColumnStr) :: restLines =>
        println((severityStr, path, startLineStr, startColumnStr))
        val prefix = s"[$severityStr] "
        val (problemLines, restLines1) = restLines.span { line =>
          line.startsWith(prefix) && !headerLine.matches(line)
        }
        val severity = parseSeverity(severityStr)
        val optProblem =
          for
            startLine <- startLineStr.toIntOption
            startColumn <- startColumnStr.toIntOption
            if problemLines.nonEmpty
          yield
            parseProblem(severity, path, startLine, startColumn, problemLines.map(_.stripPrefix(prefix)))
        optProblem.toList ::: parseFromLogs(restLines)

      case _ :: restLines =>
        parseFromLogs(restLines)

      case Nil =>
        Nil
  end parseFromLogs

  private def parseSeverity(severityStr: String): Severity = severityStr match
    case "info"  => Severity.Info
    case "warn"  => Severity.Warning
    case "error" => Severity.Error
    case _       => Severity.Error // when in doubt, it's an error
  end parseSeverity

  private def parseProblem(severity: Severity, path: String, startLine: Int, startColumn: Int, problemLines: List[String]): Problem =
    val sourceName =
      val path1 = path.replace('\\', '/')
      val idx = path1.lastIndexOf('/')
      path1.substring(idx + 1)

    val (textLines, endColumn) = problemLines match
      case textLines :+ codeLine :+ caretLine if caretLine.contains("^") && caretLine.trim().forall(_ == '^') =>
        val endColumn = caretLine.lastIndexOf('^') + 1 + 1 // second +1 to be 1-based
        (textLines, Math.max(endColumn, startColumn + 1))
      case _ =>
        (problemLines, startColumn + 1)

    Problem(severity, sourceName, startLine, startColumn, endColumn, textLines.mkString("\n"))
  end parseProblem
end Problem
