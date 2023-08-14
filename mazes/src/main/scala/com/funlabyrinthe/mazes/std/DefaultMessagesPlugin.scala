package com.funlabyrinthe.mazes
package std

import cps.customValueDiscard

import com.funlabyrinthe.core._
import graphics._
import input._

import scala.collection.mutable

class DefaultMessagesPlugin(using ComponentInit) extends MessagesPlugin {

  val optionss: Player.immutable.SimplePerPlayerData[Options] =
    new Player.immutable.SimplePerPlayerData(new Options(_))

  protected val states: Player.immutable.SimplePerPlayerData[State] =
    new Player.immutable.SimplePerPlayerData(new State(_))

  override def showMessage(player: Player, message: String): Control[Unit] = {
    val state = states(player)
    import state._

    // Configure state
    text = message
    answers = Nil
    selected = 0
    showOnlySelected = false

    // Launch
    doShowMessage(state).map(_ => ())
  }

  def showSelectionMessage(
    player: Player,
    prompt: String,
    answers: List[String],
    options: ShowSelectionMessage.Options,
  ): Control[Int] =
    val state = states(player)

    // Configure state
    state.text = prompt
    state.answers = answers
    state.selected = options.default
    state.showOnlySelected = options.showOnlySelected

    // Launch
    doShowMessage(state)
  end showSelectionMessage

  override def drawView(player: Player, context: DrawContext): Unit = {
    val state = states(player)
    import state._

    if (state.activated) {
      drawBorder(context, state)
      drawText(context, state)
      if (showAnswers)
        drawAnswers(context, state)
      else
        drawContinueSymbol(context, state)
    }
  }

  def doShowMessage(state: State): Control[Int] = control {
    import state._
    import options.{ player => _, _ }

    fixupConfig()
    prepare(state)

    activate()

    // Show message
    val displayAnswerCount = if (showOnlySelected) 1 else answerRowCount

    def showLinesLoop(): Control[Unit] = control {
      val linesLeft = lines.size - currentIndex
      val shouldProceedToAnswers =
        hasAnswers && (linesLeft + displayAnswerCount <= lineCount)

      if (!shouldProceedToAnswers) {
        waitForContinueKey(state)
        nextLines()
        if (currentIndex < lines.size)
          showLinesLoop()
      }
    }
    showLinesLoop()

    // Show answers
    if (hasAnswers) {
      showAnswers = true
      def showAnswersLoop(): Control[Unit] = control {
        exec(waitForSelectionKey(state)) match {
          case Left(direction) =>
            applySelectionDirection(state, direction)
            showAnswersLoop()
          case Right(_) =>
            ()
        }
      }
      showAnswersLoop()
    }

    // Finalization
    deactivate()

    state.selected
  }

  def prepare(state: State): Unit = {
    import state._
    import options.{ player => _, _ }

    // Fetch view size
    val (viewWidth, viewHeight) = player.controller.viewSize

    // Prepare lines and answers
    maxLineWidth = viewWidth - padding.left - padding.right
    prepareLines(state)
    if (hasAnswers) prepareAnswers(state)
    else answerRowCount = 0

    // Compute the number of lines to display in one step
    val neededLineCount = lines.size + answerRowCount
    lineCount = Math.min(maxLineCount, Math.max(minLineCount, neededLineCount))

    // Build message rect
    val rectHeight = (padding.top + padding.bottom +
        lineCount * measureText("A", font)._2)
    messageRect = new Rectangle2D(
        0, viewHeight-rectHeight, viewWidth, rectHeight)
  }

  def prepareLines(state: State): Unit = {
    import state._
    import options.{ player => _, _ }

    val pageBreakChars = Set[Char](11, 12)
    val lineBreakChars = Set[Char](10, 11, 12, 13)
    val wordBreakChars = Set[Char](9, 10, 11, 12, 13, ' ')

    val linesBuilder = new mutable.ListBuffer[String]
    val length = text.length

    var lineBeginIndex = 0
    var lastGoodIndex = -1
    var index = 0
    while (index < length) {
      while (index < length && !wordBreakChars.contains(text(index)))
        index += 1

      val currentWidth =
        measureText(text.substring(lineBeginIndex, index), font)._1

      if (currentWidth <= maxLineWidth || lastGoodIndex == -1)
        lastGoodIndex = index

      if (index >= length || lineBreakChars.contains(text(index)) ||
          currentWidth > maxLineWidth) {
        index = lastGoodIndex
        linesBuilder += text.substring(lineBeginIndex, index)

        lineBeginIndex = index+1
        lastGoodIndex = -1
      }

      if (index < length && pageBreakChars.contains(text(index))) {
        while (linesBuilder.size % maxLineCount != 0)
          linesBuilder += ""
      }

      index += 1
    }

    lines = linesBuilder.result()
  }

  def prepareAnswers(state: State): Unit = {
    import state._
    import options.{ player => _, _ }

    def divCeil(a: Int, b: Int) = (a+b-1) / b

    if (showOnlySelected) {
      answerColCount = 1
      answerRowCount = answers.size
    } else {
      val maxAnswerWidth = answers.map(measureText(_, font)._1).max
      val maxLineWidth =
        player.controller.viewSize._1 - padding.left - padding.bottom

      answerColCount = Math.max(((maxLineWidth + colSepWidth) /
          (selBulletWidth + maxAnswerWidth + colSepWidth)).toInt, 1)

      answerRowCount = divCeil(answers.size, answerColCount)

      while (answerColCount > 1 &&
          divCeil(answers.size, answerColCount-1) == answerRowCount)
        answerColCount -= 1
    }
  }

  def applySelectionDirection(state: State, dir: Direction): Unit = {
    import state._

    val prevSelX = selected % answerColCount
    val prevSelY = selected / answerColCount

    val (newSelX, newSelY) = dir match {
      case Direction.North => (prevSelX, prevSelY-1)
      case Direction.East => (prevSelX+1, prevSelY)
      case Direction.South => (prevSelX, prevSelY+1)
      case Direction.West => (prevSelX-1, prevSelY)
    }

    if (newSelX >= 0 && newSelX < answerColCount) {
      val newSelected = newSelY*answerColCount + newSelX
      if (newSelected >= 0 && newSelected < answers.size)
        selected = newSelected
    }
  }

  def drawBorder(context: DrawContext, state: State): Unit = {
    import context._
    import state.{ messageRect => rect, _ }
    import options.{ player => _, _ }

    gc.save()
    gc.lineWidth = 3
    gc.fill = backgroundColor
    gc.fillRect(rect.minX, rect.minY, rect.width, rect.height)
    gc.stroke = borderColor
    gc.strokeRect(rect.minX, rect.minY, rect.width, rect.height)
    gc.restore()
  }

  def drawText(context: DrawContext, state: State): Unit = {
    import context._
    import state._
    import options.{ player => _, _ }

    val left = messageRect.minX + padding.left
    val top = messageRect.minY + padding.top
    val lineHeight = measureText("A", font)._2

    gc.font = font
    gc.fill = textColor

    val linesLeft = lines.drop(currentIndex)

    for (i <- 0 until lineCount; if i < linesLeft.size) {
      gc.fillText(linesLeft(i), left, top + i*lineHeight)
    }
  }

  def drawAnswers(context: DrawContext, state: State): Unit = {
    import context._
    import state._
    import options.{ player => _, _ }

    // Setup font
    setupFont(gc, options)

    // Measures
    val usefulRect = messageRect.paddedInner(padding)
    val lineHeight = measureText("A", font)._2
    val colWidth = (usefulRect.width + colSepWidth) / answerColCount

    // Base is below the text lines that are still displayed
    val textLinesLeft = Math.max(0, lines.size - currentIndex)
    val base = usefulRect.topLeft + Point2D(0, lineHeight * textLinesLeft)

    // Compute the range of rows that we must display
    val shownRowCount =
      if showOnlySelected then 1
      else lineCount - textLinesLeft
    val baseIndex =
      val itemsPerPage = answerColCount * shownRowCount
      (selected / itemsPerPage) * itemsPerPage // round down to a multiple of itemsPerPage

    // Draw the answers
    for
      row <- 0 until shownRowCount
      col <- 0 until answerColCount
    do
      val index = baseIndex + row * answerColCount + col
      if index < answers.size then
        val itemPos = base + Point2D(col * colWidth, row * lineHeight)
        if index == selected then
          drawSelectionBullet(gc, itemPos)
        val textPos = itemPos + Point2D(selBulletWidth, 0)
        gc.fillText(answers(index), textPos.x, textPos.y)
    end for
  }

  private def setupFont(gc: GraphicsContext, options: Options): Unit =
    gc.font = options.font
    gc.fill = options.textColor
  end setupFont

  def drawContinueSymbol(context: DrawContext, state: State): Unit =
    import context.*
    import state.*

    val blinkedOut = (universe.tickCount % 1200L) < 600L
    if !blinkedOut then
      val Point2D(x, y) = messageRect.bottomRight - Point2D(9, 9)

      gc.moveTo(x - 3, y - 3)
      gc.lineTo(x + 3, y - 3)
      gc.lineTo(x, y + 4)
      gc.closePath()
      gc.fillPath()
  end drawContinueSymbol

  def drawSelectionBullet(gc: GraphicsContext, itemPos: Point2D): Unit = {
    // TODO Circle
    gc.fillRect(itemPos.x + 2, itemPos.y + 5, 8, 8)
  }

  def waitForContinueKey(state: State): Control[Unit] = control {
    val keyEvent = exec(waitForKeyEvent())
    if (!isContinueKeyEvent(keyEvent))
      waitForContinueKey(state)
  }

  def isContinueKeyEvent(keyEvent: KeyEvent): Boolean = {
    !keyEvent.hasAnyControlKey && isContinueKeyCode(keyEvent.code)
  }
  private val isContinueKeyCode = Set[KeyCode](KeyCode.Enter, KeyCode.Down)

  def waitForSelectionKey(state: State): Control[Either[Direction, Unit]] = control {
    val result = keyEventToSelectionOp(exec(waitForKeyEvent()))
    if (result.isDefined)
      result.get
    else
      exec(waitForSelectionKey(state))
  }

  def keyEventToSelectionOp(
      keyEvent: KeyEvent): Option[Either[Direction, Unit]] = {
    if (keyEvent.hasAnyControlKey) {
      None
    } else {
      keyEvent.code match {
        case KeyCode.Up    => Some(Left(Direction.North))
        case KeyCode.Right => Some(Left(Direction.East))
        case KeyCode.Down  => Some(Left(Direction.South))
        case KeyCode.Left  => Some(Left(Direction.West))
        case KeyCode.Enter => Some(Right(()))
        case _ => None
      }
    }
  }

  protected def measureText(text: String, font: Font) =
    universe.graphicsSystem.measureText(text, font)

  class Options(val player: Player) {
    var minLineCount: Int = 2
    var maxLineCount: Int = 3

    var font: Font = Font(List("Courier New"), 16)
    var padding: Insets = Insets(4, 10, 4, 10)
    var selBulletWidth: Double = 15
    var colSepWidth: Double = 15

    var backgroundColor: Color = Color.White
    var borderColor: Color = Color.Black
    var textColor: Color = Color.Black
  }

  // should be protected, but this will be annoying
  class State(val player: Player) {
    val options = optionss(player)

    // Configuration provided by the caller of showMessage() et al.
    var text: String = ""
    var answers: List[String] = Nil
    var selected: Int = 0
    var showOnlySelected: Boolean = false

    final def hasAnswers: Boolean = !answers.isEmpty

    // Private state

    var activated: Boolean = false

    var maxLineWidth: Double = _
    var messageRect: Rectangle2D = Rectangle2D.Empty
    var lines: List[String] = Nil
    var currentIndex: Int = _

    var lineCount: Int = _
    var showAnswers: Boolean = _
    var answerColCount: Int = _
    var answerRowCount: Int = _

    def fixupConfig() = {
      text = text.replace("\r\n", "\n")
      if (!hasAnswers)
        showOnlySelected = false
    }

    def activate() = {
      currentIndex = 0
      showAnswers = false
      activated = true
    }

    def nextLines() = {
      currentIndex += lineCount
    }

    def deactivate() = {
      activated = false
    }
  }
}
