package com.funlabyrinthe.core.messages

import scala.collection.mutable

import com.funlabyrinthe.core.*
import com.funlabyrinthe.core.input.*
import com.funlabyrinthe.core.scene.*
import com.funlabyrinthe.core.pickling.Pickleable
import com.funlabyrinthe.core.inspecting.Inspectable

class DefaultMessagesPlugin(using ComponentInit) extends MessagesPlugin:
  import DefaultMessagesPlugin.*

  var options: Options = Options.Defaults

  private var states: mutable.HashMap[CorePlayer, State] = mutable.HashMap.empty

  protected def activate[A](player: CorePlayer)(op: State => A): A = {
    val savedState = states.get(player)
    val state = new State(player, options)
    states(player) = state
    try
      op(state)
    finally
      states.updateWith(player)(_ => savedState)
  }

  protected def getCurrentState(player: CorePlayer): Option[State] =
    states.get(player)

  override def showMessage(player: CorePlayer, message: String): Unit = {
    activate(player) { state =>
      import state.*

      // Configure state
      text = message
      answers = Nil
      selected = 0
      showOnlySelected = false

      // Launch
      doShowMessage(state)
    }
  }

  def showSelectionMessage(
    player: CorePlayer,
    prompt: String,
    answers: List[String],
    options: ShowSelectionMessage.Options,
  ): Int =
    activate(player) { state =>
      // Configure state
      state.text = prompt
      state.answers = answers
      state.selected = options.default
      state.showOnlySelected = options.showOnlySelected

      // Launch
      doShowMessage(state)
    }
  end showSelectionMessage

  override def presentView(player: CorePlayer, viewSize: Size): SceneUpdateFragment = {
    getCurrentState(player) match {
      case None =>
        SceneUpdateFragment.empty

      case Some(state) =>
        import state.*
        val border = presentBorder(viewSize, state)
        val text = presentText(viewSize, state)
        val next =
          if showAnswers then presentAnswers(viewSize, state)
          else presentContinueSymbol(state)
        SceneUpdateFragment(border ++ text ++ next)
    }
  }

  def doShowMessage(state: State): Int = {
    import state.*
    import state.options.*

    fixupConfig()
    prepare(state)

    // Show message
    val displayAnswerCount = if (showOnlySelected) 1 else answerRowCount

    def showLinesLoop(): Unit = {
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
      def showAnswersLoop(): Unit = {
        waitForSelectionKey(state) match {
          case Left(direction) =>
            applySelectionDirection(state, direction)
            showAnswersLoop()
          case Right(_) =>
            ()
        }
      }
      showAnswersLoop()
    }

    state.selected
  }

  def prepare(state: State): Unit = {
    import state.*
    import state.options.*

    // Fetch view size
    val Size(viewWidth, viewHeight) = player.controller.viewSize

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
        lineCount * measureText("A", FontKey(font))._2)
    messageRect = Rectangle.ltwh(0, viewHeight-rectHeight, viewWidth, rectHeight)
  }

  def prepareLines(state: State): Unit = {
    import state.*
    import state.options.*

    val fontKey = FontKey(font)

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
        measureText(text.substring(lineBeginIndex, index), fontKey)._1

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
    import state.*
    import state.options.*

    def divCeil(a: Int, b: Int) = (a+b-1) / b

    if (showOnlySelected) {
      answerColCount = 1
      answerRowCount = answers.size
    } else {
      val maxAnswerWidth = answers.map(measureText(_, FontKey(font))._1).max
      val maxLineWidth =
        player.controller.viewSize._1 - padding.left - padding.bottom

      answerColCount = Math.max(
          (maxLineWidth + colSepWidth) / (selBulletWidth + maxAnswerWidth + colSepWidth), 1)

      answerRowCount = divCeil(answers.size, answerColCount)

      while (answerColCount > 1 &&
          divCeil(answers.size, answerColCount-1) == answerRowCount)
        answerColCount -= 1
    }
  }

  def applySelectionDirection(state: State, dir: Direction): Unit = {
    import state.*

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

  private def presentBorder(viewSize: Size, state: State): Batch[SceneNode] = {
    import state.*
    import state.messageRect as rect
    import state.options.*

    Batch(
      Shape.Box(
        Rectangle.ltwh(rect.left, rect.top, rect.width, rect.height),
        Fill.Color(backgroundColor),
        Stroke(3, borderColor)
      )
    )
  }

  private def presentText(viewSize: Size, state: State): Batch[SceneNode] = {
    import state.*
    import state.options.*

    val key = FontKey(font)

    val rect = computeUsefulRect(messageRect, padding)
    val lineHeight = measureText("A", key)._2

    val linesToDisplay = lines.drop(currentIndex).take(lineCount)

    Batch.from(
      for (line, i) <- linesToDisplay.zipWithIndex yield
        Text(Point(rect.left, rect.top + i * lineHeight), line, key, textColor, Point.zero)
    )
  }

  private def computeUsefulRect(messageRect: Rectangle, padding: Padding): Rectangle = {
    Rectangle.ltwh(
      messageRect.left + padding.left,
      messageRect.top + padding.top,
      messageRect.width - padding.left - padding.right,
      messageRect.height - padding.top - padding.bottom
    )
  }

  private def presentAnswers(viewSize: Size, state: State): Batch[SceneNode] = {
    import state.*
    import state.options.*

    val key = FontKey(font)

    // Measures
    val usefulRect = computeUsefulRect(messageRect, padding)
    val lineHeight = measureText("A", key)._2
    val colWidth = (usefulRect.width + colSepWidth) / answerColCount

    // Base is below the text lines that are still displayed
    val textLinesLeft = Math.max(0, lines.size - currentIndex)
    val base = usefulRect.topLeft + Point(0, lineHeight * textLinesLeft)

    // Compute the range of rows that we must display
    val shownRowCount =
      if showOnlySelected then 1
      else lineCount - textLinesLeft
    val baseIndex =
      val itemsPerPage = answerColCount * shownRowCount
      (selected / itemsPerPage) * itemsPerPage // round down to a multiple of itemsPerPage

    // Draw the answers

    val presentedSelectionBullet =
      val relIndex = selected - baseIndex
      val selRow = relIndex / answerColCount
      val selCol = relIndex % answerColCount
      val selPos = base + Point(selCol * colWidth, selRow * lineHeight)
      presentSelectionBullet(state, selPos)

    val presentedAnswers = Batch.from(
      for {
        row <- 0 until shownRowCount
        col <- 0 until answerColCount
        index = baseIndex + row * answerColCount + col
        if index < answers.size
      } yield {
        val itemPos = base + Point(col * colWidth, row * lineHeight)
        val textPos = itemPos + Point(selBulletWidth, 0)
        Text(textPos, answers(index), key, textColor, Point.zero)
      }
    )

    presentedAnswers ++ presentedSelectionBullet
  }

  private def presentContinueSymbol(state: State): Batch[SceneNode] = {
    import state.*
    import state.options.*

    val blinkedOut = (universe.tickCount % 1200L) < 600L
    if blinkedOut then
      Batch.empty
    else
      val fill = Fill.Color(borderColor)
      val base = Point(messageRect.right, messageRect.bottom) - 9
      val vertices = Batch(base - Point(3, 3), base + Point(3, -3), base + Point(0, 4))
      Batch(Shape.Polygon(vertices, fill))
  }

  private def presentSelectionBullet(state: State, itemPos: Point): Batch[SceneNode] =
    val fill = Fill.Color(state.options.borderColor)
    Batch(Shape.Circle(Circle(itemPos + Point(6, 9), 4), fill))

  def waitForContinueKey(state: State): Unit = {
    val keyEvent = state.player.waitForKeyEvent()
    if (!isContinueKeyEvent(keyEvent))
      waitForContinueKey(state)
  }

  def isContinueKeyEvent(keyEvent: KeyEvent): Boolean = {
    !keyEvent.hasAnyControlKey && isContinueKeyString(keyEvent.keyString)
  }
  private val isContinueKeyString: Set[String] =
    Set(KeyStrings.Enter, KeyStrings.ArrowDown)

  def waitForSelectionKey(state: State): Either[Direction, Unit] = {
    val result = keyEventToSelectionOp(state.player.waitForKeyEvent())
    if (result.isDefined)
      result.get
    else
      waitForSelectionKey(state)
  }

  def keyEventToSelectionOp(
      keyEvent: KeyEvent): Option[Either[Direction, Unit]] = {
    if (keyEvent.hasAnyControlKey) {
      None
    } else {
      keyEvent.keyString match {
        case KeyStrings.ArrowUp    => Some(Left(Direction.North))
        case KeyStrings.ArrowRight => Some(Left(Direction.East))
        case KeyStrings.ArrowDown  => Some(Left(Direction.South))
        case KeyStrings.ArrowLeft  => Some(Left(Direction.West))
        case KeyStrings.Enter      => Some(Right(()))
        case _                     => None
      }
    }
  }

  protected def measureText(text: String, fontKey: FontKey): (Int, Int) =
    //universe.graphicsSystem.measureText(text, font)
    (text.length() * CharWidth, LineHeight)
end DefaultMessagesPlugin

object DefaultMessagesPlugin:
  // TODO Fix font measurement
  private final val LineHeight = 16 + 2
  private final val CharWidth = 8

  final case class Options(
    minLineCount: Int,
    maxLineCount: Int,
    font: String,
    padding: Padding,
    selBulletWidth: Int,
    colSepWidth: Int,
    backgroundColor: RGBA,
    borderColor: RGBA,
    textColor: RGBA,
  ) derives Pickleable, Inspectable

  object Options {
    val Defaults: Options = Options(
      minLineCount = 2,
      maxLineCount = 3,
      font = "default-font",
      padding = Padding(4, 10, 4, 10),
      selBulletWidth = 15,
      colSepWidth = 15,
      backgroundColor = RGBA.White,
      borderColor = RGBA.Black,
      textColor = RGBA.Black,
    )
  }

  // should be protected, but this will be annoying
  class State(val player: CorePlayer, val options: Options) {
    // Configuration provided by the caller of showMessage() et al.
    var text: String = ""
    var answers: List[String] = Nil
    var selected: Int = 0
    var showOnlySelected: Boolean = false

    final def hasAnswers: Boolean = !answers.isEmpty

    // Private state

    var maxLineWidth: Double = 0.0
    var messageRect: Rectangle = Rectangle.sized(0, 0)
    var lines: List[String] = Nil
    var currentIndex: Int = 0

    var lineCount: Int = 0
    var showAnswers: Boolean = false
    var answerColCount: Int = 0
    var answerRowCount: Int = 0

    def fixupConfig() = {
      text = text.replace("\r\n", "\n")
      if (!hasAnswers)
        showOnlySelected = false
    }

    def nextLines() = {
      currentIndex += lineCount
    }
  }
end DefaultMessagesPlugin
