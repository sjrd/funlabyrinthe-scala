package com.funlabyrinthe.editor.renderer

import scala.scalajs.js

import com.funlabyrinthe.scene.*

import com.funlabyrinthe.htmlenv.ResourceLoader
import com.funlabyrinthe.graphics.html.GraphicsContextWrapper

import org.scalajs.dom

object SceneRenderer {
  enum Anchor {
    case TopLeft
    case Center
  }

  def renderSceneToImageBitmap(resourceLoader: ResourceLoader,
      fragment: SceneUpdateFragment, size: Size, anchor: Anchor = Anchor.TopLeft): dom.ImageBitmap = {
    renderSceneToOffscreenCanvas(resourceLoader, fragment, size, anchor).transferToImageBitmap()
  }

  def renderSceneToOffscreenCanvas(resourceLoader: ResourceLoader,
      fragment: SceneUpdateFragment, size: Size, anchor: Anchor = Anchor.TopLeft): dom.OffscreenCanvas = {
    val canvas = new dom.OffscreenCanvas(size.width, size.height)
    val gc = canvas.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]
    anchor match
      case Anchor.TopLeft => ()
      case Anchor.Center  => gc.translate(size.width / 2, size.height / 2)

    gc.font = "400 16px Roboto"

    new SceneRenderer(resourceLoader, gc, size).renderSceneUpdateFragment(fragment)
    canvas
  }
}

private final class SceneRenderer(
  resourceLoader: ResourceLoader,
  gc: dom.CanvasRenderingContext2D,
  canvasSize: Size,
) {
  private val gcWrapper = new GraphicsContextWrapper(gc)

  def renderSceneUpdateFragment(fragment: SceneUpdateFragment): Unit = {
    for layer <- fragment.layers do
      renderLayer(layer)
  }

  private def renderLayer(layer: Layer): Unit = {
    // TODO Should we deal with layer.blending?
    renderBatchOfSceneNodes(layer.nodes)
  }

  private def renderBatchOfSceneNodes(batch: Batch[SceneNode]): Unit =
    batch.foreach(renderSceneNode(_))

  private def renderSceneNode(node: SceneNode): Unit = {
    node match
      case node: Graphic =>
        renderGraphic(node)

      case Group(children, position, rotation, ref) =>
        // TODO Handle rotation
        gc.save()
        gc.translate(position.x - ref.x, position.y - ref.y)
        for child <- children do
          renderSceneNode(child)
        gc.restore()

      case Shape.Box(dimensions, fill, stroke, ref) =>
        gc.save()
        setupFill(fill)
        setupStroke(stroke)
        gc.fillRect(dimensions.topLeft.x - ref.x, dimensions.topLeft.y - ref.y,
            dimensions.size.width, dimensions.size.height)
        gc.restore()

      case Shape.Circle(circle, fill, stroke, ref) =>
        gc.save()
        setupFill(fill)
        setupStroke(stroke)
        gc.beginPath()
        gc.arc(circle.center.x - ref.x, circle.center.y - ref.y, circle.radius, 0.0, 2.0 * Math.PI)
        gc.fill()
        gc.restore()

      case Shape.Line(start, end, stroke, ref) =>
        gc.save()
        setupStroke(stroke)
        gc.beginPath()
        gc.moveTo(start.x - ref.x, start.y - ref.y)
        gc.lineTo(end.x - ref.x, end.y - ref.y)
        gc.stroke()
        gc.restore()

      case Shape.Polygon(vertices, fill, stroke, ref) =>
        gc.save()
        setupFill(fill)
        setupStroke(stroke)
        gc.beginPath()
        gc.moveTo(vertices.last.x - ref.x, vertices.last.y - ref.y)
        for vertex <- vertices do
          gc.lineTo(vertex.x - ref.x, vertex.y - ref.y)
        gc.fill()
        gc.restore()

      case Text(pos, text, font, textColor, ref) =>
        // TODO Handle font
        gc.save()
        setupFill(Fill.Color(textColor))
        gc.textBaseline = "top"
        gc.fillText(text, pos.x - ref.x, pos.y - ref.y)

      case Masked(mask, child) =>
        // TODO
        /*val renderedMask =
          SceneRenderer.renderSceneToImageBitmap(resourceLoader, SceneUpdateFragment(Batch(mask)), canvasSize)
        val nestedCanvas = new dom.OffscreenCanvas(SquareSize, SquareSize)
          val ctx = new DissipateNeighborsDrawSquareContext(
            canvas.getGraphicsContext2D(),
            context.tickCount,
            Rectangle2D(0, 0, SquareSize, SquareSize),
            context.where,
            context.purpose,
          )
          (canvas, ctx)

        def dissipateOne(field: Field, gradient: Paint): Unit =
          val gc = nestedContext.gc
          gc.save()
          nestedContext.gc.clearRect(0, 0, SquareSize, SquareSize)
          field.drawTo(nestedContext)
          gc.globalCompositeOperation = GlobalCompositeOperation.DestinationOut
          gc.fill = gradient
          gc.fillRect(0, 0, SquareSize, SquareSize)
          gc.restore()

          context.gc.drawImage(nestedCanvas, context.tickCount, context.rect.minX, context.rect.minY)
        end dissipateOne

        println("Warning: unsupported nested Masked node")
        Shape.Box(Rectangle(Size(1, 1)), Fill.None)*/
        ()
  }

  private def setupFill(fill: Fill): Unit = {
    gc.fillStyle = fill match
      case Fill.Color(color) =>
        convertRGBA(color)
      case Fill.LinearGradient(fromPoint, fromColor, toPoint, toColor) =>
        val gradient =
          gc.createLinearGradient(fromPoint.x, fromPoint.y, toPoint.x, toPoint.y)
        gradient.addColorStop(0.0, convertRGBA(fromColor))
        gradient.addColorStop(1.0, convertRGBA(toColor))
        gradient
      case Fill.RadialGradient(fromPoint, fromColor, toPoint, toColor) =>
        val diffX = toPoint.x - fromPoint.x
        val diffY = toPoint.y - fromPoint.y
        val radius = Math.sqrt(diffX*diffX + diffY*diffY)
        val gradient =
          gc.createRadialGradient(fromPoint.x, fromPoint.y, 0.0, fromPoint.x, fromPoint.y, radius)
        gradient.addColorStop(0.0, convertRGBA(fromColor))
        gradient.addColorStop(1.0, convertRGBA(toColor))
        gradient
  }

  private def setupStroke(stroke: Stroke): Unit = {
    val Stroke(width, color) = stroke
    gc.strokeStyle = convertRGBA(color)
    // TODO Can we handle `width`?
  }

  private def convertRGBA(rgba: RGBA): String = {
    import com.funlabyrinthe.graphics.html.Conversions.{ coreColorComponent2html => coreCC2html }
    s"rgba(${coreCC2html(rgba.red)},${coreCC2html(rgba.green)},${coreCC2html(rgba.blue)},${rgba.alpha})"
  }

  private def renderGraphic(graphic: Graphic): Unit = {
    val Graphic(material, crop, position, ref) = graphic
    val Material(asset, alpha, tint) = material

    // FIX Handle alpha and tint

    resourceLoader.loadImage(asset) match {
      case Some(image) if image.isComplete =>
        gcWrapper.drawImage(image, tickCount = 0L,
            crop.topLeft.x, crop.topLeft.y, crop.size.width, crop.size.height,
            position.x - ref.x, position.y - ref.y, crop.size.width, crop.size.height)

      case _ =>
        ()
    }
  }
}
