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
  private val gc: dom.CanvasRenderingContext2D,
  canvasSize: Size,
) {
  private val gcWrapper = new GraphicsContextWrapper(gc)

  private def makeSubCanvas(): (dom.OffscreenCanvas, SceneRenderer) = {
    val subCanvas = new dom.OffscreenCanvas(canvasSize.width, canvasSize.height)
    val subGC = subCanvas.getContext("2d", new dom.TwoDContextAttributes {
      willReadFrequently = true
    }).asInstanceOf[dom.CanvasRenderingContext2D]
    subGC.font = gc.font
    val subRenderer = new SceneRenderer(resourceLoader, subGC, canvasSize)
    (subCanvas, subRenderer)
  }

  private lazy val (maskCanvas, maskRenderer) = makeSubCanvas()
  private lazy val (maskedChildCanvas, maskedChildRenderer) = makeSubCanvas()

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
        gc.save()
        gc.translate(position.x, position.y)
        gc.rotate(rotation.toDouble)
        gc.translate(-ref.x, -ref.y)
        for child <- children do
          renderSceneNode(child)
        gc.restore()

      case Shape.Box(dimensions, fill, stroke, ref) =>
        gc.save()
        setupFill(fill, ref)
        setupStroke(stroke)
        gc.fillRect(dimensions.topLeft.x - ref.x, dimensions.topLeft.y - ref.y,
            dimensions.size.width, dimensions.size.height)
        gc.restore()

      case Shape.Circle(circle, fill, stroke, ref) =>
        gc.save()
        setupFill(fill, ref)
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
        setupFill(fill, ref)
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
        setupFill(Fill.Color(textColor), ref)
        gc.textBaseline = "top"
        gc.fillText(text, pos.x - ref.x, pos.y - ref.y)
        gc.restore()

      case Masked(mask, child) =>
        maskRenderer.gc.save()
        maskRenderer.gc.clearRect(0, 0, canvasSize.width, canvasSize.height)
        maskRenderer.gc.asInstanceOf[js.Dynamic].setTransform(gc.asInstanceOf[js.Dynamic].getTransform())
        maskRenderer.renderBatchOfSceneNodes(Batch(mask))
        maskRenderer.gc.restore()

        maskedChildRenderer.gc.save()
        maskedChildRenderer.gc.clearRect(0, 0, canvasSize.width, canvasSize.height)
        maskedChildRenderer.gc.asInstanceOf[js.Dynamic].setTransform(gc.asInstanceOf[js.Dynamic].getTransform())
        maskedChildRenderer.renderBatchOfSceneNodes(Batch(child))
        maskedChildRenderer.gc.restore()
        maskedChildRenderer.gc.globalCompositeOperation = "destination-in"
        maskedChildRenderer.gc.drawImage(maskCanvas.asInstanceOf[dom.HTMLElement], 0, 0)
        maskedChildRenderer.gc.globalCompositeOperation = "source-over"

        gc.save()
        gc.setTransform(1, 0, 0, 1, 0, 0)
        gc.drawImage(maskedChildCanvas.asInstanceOf[dom.HTMLElement], 0, 0)
        gc.restore()
  }

  private def setupFill(fill: Fill, ref: Point): Unit = {
    gc.fillStyle = fill match
      case Fill.Color(color) =>
        convertRGBA(color)
      case Fill.LinearGradient(fromPoint, fromColor, toPoint, toColor) =>
        val gradient =
          gc.createLinearGradient(fromPoint.x - ref.x, fromPoint.y - ref.y, toPoint.x - ref.x, toPoint.y - ref.y)
        gradient.addColorStop(0.0, convertRGBA(fromColor))
        gradient.addColorStop(1.0, convertRGBA(toColor))
        gradient
      case Fill.RadialGradient(fromPoint, fromColor, toPoint, toColor) =>
        val diffX = toPoint.x - fromPoint.x
        val diffY = toPoint.y - fromPoint.y
        val radius = Math.sqrt(diffX*diffX + diffY*diffY)
        val gradient =
          gc.createRadialGradient(fromPoint.x - ref.x, fromPoint.y - ref.y, 0.0, fromPoint.x - ref.x, fromPoint.y - ref.y, radius)
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
