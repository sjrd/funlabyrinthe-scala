package com.funlabyrinthe.graphics.html

import java.nio.{ByteBuffer, ByteOrder}
import java.util.Arrays

import scala.collection.mutable

import scala.scalajs.js
import scala.scalajs.js.typedarray.*
import scala.scalajs.js.typedarray.TypedArrayBufferOps.*

import org.scalajs.dom.{Blob, BlobPart, BlobPropertyBag}

/* Ported from
 * https://github.com/davidmz/apng-js/blob/52f6fab62ffabe2abac3467d6abf82fe98ba4018/src/library/parser.js
 * Copyright (c) 2016 David Mzareulyan -- MIT License
 *
 * We didn't reuse it as is, because we actually want to support
 * non-animated PNG as well, in a single run.
 */

object PNGParser:
  final class PNGInfo:
    var width: Int = 0
    var height: Int = 0

    var isAnimated: Boolean = false

    var numFrames: Int = 0
    var numPlays: Int = 0
    var playTime: Int = 0

    val frames = mutable.ArrayBuffer.empty[Frame]

    override def toString(): String =
      s"""
        |PNGInfo(
        |  width = $width,
        |  height = $height,
        |  isAnimated = $isAnimated,
        |  numFrames = $numFrames,
        |  numPlays = $numPlays,
        |  playTime = $playTime,
        |  frames = ${frames.mkString("[\n    ", ",\n    ", ",\n  ]")},
        |)
      """.stripMargin.trim()
  end PNGInfo

  final class Frame:
    var width: Int = 0
    var height: Int = 0
    var left: Int = 0
    var top: Int = 0
    var delay: Int = 0
    var disposeOp: Int = 0
    var blendOp: Int = 0

    val dataParts = mutable.ArrayBuffer.empty[ByteBuffer]
    var imageData: Option[Blob] = None

    override def toString(): String =
      s"""
        |    Frame(
        |      width = $width,
        |      height = $height,
        |      left = $left,
        |      top = $top,
        |      delay = $delay,
        |      disposeOp = $disposeOp,
        |      blendOp = $blendOp,
        |    )
      """
  end Frame

  // '\x89PNG\x0d\x0a\x1a\x0a'
  private val PNGSignature = Array[Byte](0x89.toByte, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a)

  private object Types:
    opaque type Type = Int

    object Type:
      def apply(raw: Int): Type = raw

      extension (tpe: Type)
        def toInt: Int = tpe
    end Type

    private def makeTypeConstant(name: String): Type =
      assert(name.length == 4, name)
      (name.charAt(0) << 24) | (name.charAt(1) << 16) | (name.charAt(2) << 8) | name.charAt(3)

    val IHDR: Type = makeTypeConstant("IHDR")
    val acTL: Type = makeTypeConstant("acTL")
    val fcTL: Type = makeTypeConstant("fcTL")
    val fdAT: Type = makeTypeConstant("fdAT")
    val IDAT: Type = makeTypeConstant("IDAT")
    val IEND: Type = makeTypeConstant("IEND")
  end Types

  import Types.Type

  def parse(buffer: ByteBuffer): PNGInfo =
    buffer.order(ByteOrder.BIG_ENDIAN)

    // Signature
    // https://www.w3.org/TR/png-3/#3PNGsignature
    if buffer.remaining() < PNGSignature.length || !Arrays.equals(buffer.getBytes(PNGSignature.length), PNGSignature) then
      throw IllegalArgumentException("Not a PNG file")

    val preDataParts = js.Array[BlobPart]()
    val postDataParts = js.Array[BlobPart]()

    var headerDataBytes: Option[ByteBuffer] = None
    var currentFrame: Option[Frame] = None
    var frameNumber: Int = 0

    val apng = new PNGInfo()

    forEachChunk(buffer) { (tpe, dv) =>
      def skipLengthAndType(): Unit =
        dv.position(4 + 4)

      def limitBeforeCRC(): Unit =
        dv.limit(dv.limit() - 4)

      tpe match
        case Types.IHDR =>
          // https://www.w3.org/TR/png-3/#11IHDR
          skipLengthAndType()
          limitBeforeCRC()
          dv.mark()
          apng.width = dv.getInt()
          apng.height = dv.getInt()
          // Ignore other fields
          dv.reset()
          headerDataBytes = Some(dv)

        case Types.acTL =>
          // https://www.w3.org/TR/png-3/#acTL-chunk
          skipLengthAndType()
          apng.numFrames = dv.getInt()
          apng.numPlays = dv.getInt()

        case Types.fcTL =>
          // https://www.w3.org/TR/png-3/#fcTL-chunk
          for frame <- currentFrame do
            apng.frames += frame
            frameNumber += 1

          val frame = new Frame()
          currentFrame = Some(frame)

          skipLengthAndType()
          dv.getInt() // sequence number
          frame.width = dv.getInt()
          frame.height = dv.getInt()
          frame.left = dv.getInt()
          frame.top = dv.getInt()

          val delayNumerator = dv.getShort() & 0xffff
          val delayDenominator = dv.getShort() & 0xffff
          frame.delay =
            if delayDenominator == 0 then 10 * delayNumerator // denominator is actually 100
            else 1000 * delayNumerator / delayDenominator

          // https://bugzilla.mozilla.org/show_bug.cgi?id=125137
          // https://bugzilla.mozilla.org/show_bug.cgi?id=139677
          // https://bugzilla.mozilla.org/show_bug.cgi?id=207059
          if frame.delay <= 10 then
            frame.delay = 100

          apng.playTime += frame.delay

          frame.disposeOp = dv.get() & 0xff
          frame.blendOp = dv.get() & 0xff

          if frameNumber == 0 && frame.disposeOp == 2 then
            frame.disposeOp = 1

        case Types.fdAT =>
          // https://www.w3.org/TR/png-3/#fdAT-chunk
          for frame <- currentFrame do
            skipLengthAndType()
            dv.getInt() // sequence number
            limitBeforeCRC()
            frame.dataParts += dv

        case Types.IDAT =>
          // https://www.w3.org/TR/png-3/#11IDAT
          for frame <- currentFrame do
            skipLengthAndType()
            limitBeforeCRC()
            frame.dataParts += dv

        case Types.IEND =>
          // https://www.w3.org/TR/png-3/#11IEND
          postDataParts += dv.toBlobPart

        case _ =>
          preDataParts += dv.toBlobPart
      end match

      true // continue
    }

    for frame <- currentFrame do
      apng.frames += frame

    if !apng.frames.isEmpty then
      apng.isAnimated = true

      val preBlob = new Blob(preDataParts)
      val postBlob = new Blob(postDataParts)

      val header = ByteBuffer.allocateDirect(headerDataBytes.get.remaining()).order(ByteOrder.BIG_ENDIAN)
      header.put(headerDataBytes.get)

      headerDataBytes.get.rewind()
      headerDataBytes.get.limit(headerDataBytes.get.capacity())

      var first = true

      for frame <- apng.frames do
        val bb = js.Array[BlobPart]()

        bb += PNGSignature.toTypedArray

        header.rewind()
        header.putInt(frame.width)
        header.putInt(frame.height)
        header.rewind()
        //bb += makeChunkBytes(Types.IHDR, header).toBlobPart
        val chunkBytes = makeChunkBytes(Types.IHDR, header)
        println("------------------")
        println(headerDataBytes.get)
        println(chunkBytes)
        println(chunkBytes.equals(headerDataBytes.get))
        bb += chunkBytes.toBlobPart

        bb += preBlob
        for dataPart <- frame.dataParts do
          bb += makeChunkBytes(Types.IDAT, dataPart).toBlobPart
        bb += postBlob

        frame.imageData = Some(new Blob(bb, new BlobPropertyBag { `type` = "image/png" }))
        frame.dataParts.clear() // GC
      end for
    end if

    apng
  end parse

  /**
   * @param {Uint8Array} bytes
   * @param {function(string, Uint8Array, int, int): boolean} callback
   */
  private def forEachChunk(buffer: ByteBuffer)(callback: (Type, ByteBuffer) => Boolean): Unit =
    val dv = buffer.duplicate()

    // https://www.w3.org/TR/png-3/#5Chunk-layout

    while ({
      // Read length and type without advancing the position
      val length = dv.getInt(dv.position())
      val tpe = Type(dv.getInt(dv.position() + 4))

      val continue = callback(tpe, dv.extractSubBuffer(4 + 4 + length + 4))
      continue && tpe != Types.IEND && dv.hasRemaining()
    }) {}
  end forEachChunk

  /**
   * @param {string} type
   * @param {Uint8Array} dataBytes
   * @return {Uint8Array}
   */
  private def makeChunkBytes(tpe: Type, dataBytes: ByteBuffer): ByteBuffer =
    val dataLength = dataBytes.remaining()
    val bytes = ByteBuffer.allocateDirect(4 + 4 + dataLength + 4).order(ByteOrder.BIG_ENDIAN)

    bytes.putInt(dataBytes.remaining())
    val crcStartPos = bytes.position()
    bytes.putInt(tpe.toInt)
    bytes.put(dataBytes)
    val crc = CRC32.compute(bytes.duplicate().flip().position(crcStartPos))
    bytes.putInt(crc)

    bytes.rewind()
    bytes
  end makeChunkBytes

  extension (buffer: ByteBuffer)
    private def getBytes(length: Int): Array[Byte] =
      val result = new Array[Byte](length)
      buffer.get(result)
      result

    private def extractSubBuffer(length: Int): ByteBuffer =
      val pos = buffer.position()
      val savedLimit = buffer.limit()
      buffer.limit(pos + length)
      val result = buffer.slice()
      buffer.position(buffer.limit())
      buffer.limit(savedLimit)
      result

    private def toBlobPart: BlobPart =
      buffer.typedArray().subarray(buffer.position(), buffer.limit())
  end extension
end PNGParser
