package com.funlabyrinthe.htmlenv

import scala.annotation.tailrec

import scala.compiletime.uninitialized

import scala.collection.mutable

import java.io.IOException
import java.nio.{ByteBuffer, ByteOrder}

// Ported from https://github.com/deanm/omggif/blob/0ec9069a525ec66223eadda3d62059d3685fd3b0/omggif.js#L366
// MIT License
final class GIFReader(buf: ByteBuffer):
  import GIFReader.*

  buf.order(ByteOrder.LITTLE_ENDIAN)

  private var _width: Int = uninitialized
  private var _height: Int = uninitialized
  private val _frames = mutable.ArrayBuffer.empty[Frame]
  private var _loopCount: Int = -1

  def width: Int = _width
  def height: Int = _height

  private def getUByte(): Int = buf.get() & 0xff
  private def getUShort(): Int = buf.getShort() & 0xffff

  read()

  private def read(): Unit =
    // Header (GIF87a or GIF89a)
    if buf.get() != 0x47 || buf.get() != 0x49 || buf.get() != 0x46
        || buf.get() != 0x38 || ((buf.get() + 1) & 0xfd) != 0x38 || buf.get() != 0x61 then
      throw IOException("Invalid GIF 87a/89a header")
    end if

    // Logical Screen Descriptor
    this._width = getUShort()
    this._height = getUShort()
    val pf0 = getUByte() // <Packed Fields>
    val globalPaletteFlag = (pf0 >> 7) != 0
    val numGlobalColorsPow2 = pf0 & 0x7
    val numGlobalColors = 1 << (numGlobalColorsPow2 + 1)
    val background = buf.get()
    buf.get() // Pixel aspect ratio (unused?)

    var globalPaletteOffset = -1
    var globalPaletteSize = -1
    if globalPaletteFlag then
      globalPaletteOffset = buf.position()
      globalPaletteSize = numGlobalColors
      buf.position(buf.position() + numGlobalColors * 3) // Seek past palette
    end if

    var noEOF = true

    var delay = 0
    var transparentIndex = NoTransparentColor // was null
    var disposal = 0 // 0 - No disposal specified

    @tailrec
    def skipSubblocks(): Unit =
      val blockSize = getUByte()
      if blockSize != 0 then // 0 size is terminator
        buf.position(buf.position() + blockSize)
        skipSubblocks()
    end skipSubblocks

    // Read the blocks
    while buf.remaining() > 0 && noEOF do
      getUByte() match
        // Graphics Control Extension Block
        case 0x21 =>
          getUByte() match
            // Application-specific block
            case 0xff =>
              // Try if it's a Netscape block (with animation loop counter).
              val p = buf.position()

              // NETSCAPE 2.0
              def isNetscape20Block: Boolean =
                buf.get(p+1) == 0x4e && buf.get(p+2) == 0x45 && buf.get(p+3) == 0x54
                  && buf.get(p+4) == 0x53 && buf.get(p+5) == 0x43 && buf.get(p+6) == 0x41
                  && buf.get(p+7) == 0x50 && buf.get(p+8) == 0x45 && buf.get(p+9) == 0x32
                  && buf.get(p+10) == 0x2e && buf.get(p+11) == 0x30
                  // Sub-block
                  && buf.get(p+12) == 0x03 && buf.get(p+13) == 0x01 && buf.get(p+16) == 0
              end isNetscape20Block

              // 21 FF already read, check block size; or, test the Netscape 2.0 block
              if buf.get(p) != 0x0b || isNetscape20Block then
                buf.position(p + 14)
                _loopCount = buf.getShort()
                buf.get() // Skip terminator
              else
                // We don't know what it is, just try to get past it.
                buf.position(p + 12)
                skipSubblocks()
              end if

            // Graphics Control Extension
            case 0xf9 =>
              if getUByte() != 0x04 || buf.get(buf.position() + 4) != 0 then
                throw IOException("Invalid graphics extension block")
              val pf1 = getUByte()
              delay = getUShort()
              transparentIndex = getUByte()
              if (pf1 & 1) == 0 then
                transparentIndex = NoTransparentColor
              disposal = (pf1 >> 2) & 0x07
              buf.get() // Skip terminator

            /* Plain Text Extension or Comment Extension
             * Plain Text Extension could be present and we just want to be able
             * to parse past it. It follows the block structure of the comment
             * extension enough to reuse the path to skip through the blocks.
             */
            case 0x01 | 0xfe =>
              skipSubblocks()

            case blockType =>
              throw IOException(s"Unknown graphic control label: 0x${blockType.toHexString}")

        // Image Descriptor
        case 0x2c =>
          val x = getUShort()
          val y = getUShort()
          val w = getUShort()
          val h = getUShort()
          val pf2 = getUByte()
          val localPaletteFlag = (pf2 >> 7) != 0
          val interlaceFlag = ((pf2 >> 6) & 1) != 0
          val numLocalColorsPow2 = pf2 & 0x0f
          val numLocalColors = 1 << (numLocalColorsPow2 + 1)
          var paletteOffset = globalPaletteOffset
          var paletteSize = globalPaletteSize

          val hasLocalPalette = localPaletteFlag
          if hasLocalPalette then
            paletteOffset = buf.position() // Override with local palette
            paletteSize = numLocalColors
            buf.position(buf.position() + numLocalColors * 3) // Seek past palette
          end if

          val dataOffset = buf.position()
          buf.get() // codesize
          skipSubblocks()
          val dataLength = buf.position() - dataOffset

          _frames += new Frame(
            x,
            y,
            w,
            h,
            paletteOffset,
            paletteSize,
            dataOffset,
            dataLength,
            transparentIndex,
            interlaceFlag,
            delay,
            disposal,
          )

        // Trailer Marker (end of file)
        case 0x3b =>
          noEOF = false

        case blockType =>
          throw IOException(s"Unknown gif block: 0x${blockType.toHexString}")
    end while
  end read

  def frameCount: Int = _frames.size

  def loopCount: Int = _loopCount

  def frameInfo(index: Int): Frame = _frames(index)

  def decodeAndBlitFrameRGBA(frameIndex: Int, pixels: Array[Byte]): Unit =
    val frame = frameInfo(frameIndex)
    val pixelCount = frame.width * frame.height
    val indexStream = new Array[Byte](pixelCount) // At most 8-bit indices
    buf.position(frame.dataOffset)
    gifReaderLZWOutputIndexStream(indexStream)

    val paletteOffset = frame.paletteOffset
    val transparentIndex = frame.transparentIndex

    // We are possibly just blitting to a portion of the entire frame.
    // That is a subrect within the framerect, so the additional pixels
    // must be skipped over after we finished a scanline.
    val frameWidth = frame.width
    val frameStride = width - frameWidth
    var xLeft = frameWidth // Number of subrect pixels left in scanline.

    // Output index of the top left corner of the subrect.
    val opStart = ((frame.y * width) + frame.x) * 4
    // Output index of what would be the left edge of the subrect, one row
    // below it, i.e., the index at which an interlace pass should wrap.
    val opEnd = ((frame.y + frame.height) * width + frame.x) * 4
    var op = opStart

    var scanStride = frameStride * 4

    // Use scanstride to skip past the rows when interlacing.
    // This is skipping 7 rows for the first two passes, then 3 then 1.
    if frame.interlaced then
      scanStride += width * 4 * 7 // Pass 1.

    var interlaceSkip = 8 // Tracking the row interval in the current pass.

    for i <- 0 until indexStream.length do
      val index = indexStream(i) & 0xff

      if xLeft == 0 then
        // Beginning of new scan line
        op += scanStride
        xLeft = frameWidth
        if op >= opEnd then
          // Catch the wrap to switch passes when interlacing
          scanStride = frameStride * 4 + width * 4 * (interlaceSkip - 1)
          // interlaceSkip / 2 * 4 is interlaceSkip << 1
          op = opStart + (frameWidth + frameStride) * (interlaceSkip << 1)
          interlaceSkip >>= 1
      end if

      if index != transparentIndex then
        val offset = paletteOffset + index * 3
        val r = buf.get(offset)
        val g = buf.get(offset + 1)
        val b = buf.get(offset + 2)
        pixels(op) = r
        pixels(op + 1) = g
        pixels(op + 2) = b
        pixels(op + 3) = 255.toByte // alpha is always opaque
      end if

      op += 4
      xLeft -= 1
    end for
  end decodeAndBlitFrameRGBA

  private def gifReaderLZWOutputIndexStream(output: Array[Byte]): Unit =
    val outputLength = output.length

    val minCodeSize = getUByte()

    val clearCode = 1 << minCodeSize
    val eoiCode = clearCode + 1
    var nextCode = eoiCode + 1

    var curCodeSize = minCodeSize + 1 // Number of bits per code
    // Here this masks each code coming from the code stream
    var codeMask = (1 << curCodeSize) - 1
    var curShift = 0
    var cur = 0

    var op = 0 // Output pointer.

    var subblockSize = getUByte()

    val codeTable = new Array[Int](4096) // Can be signed, we only use 20 bits

    var prevCode = Int.MinValue // Track code-1 (was null)

    var break = false
    while !break do
      // Read up to two bytes, making sure we always 12-bits for max sized code.
      // If subblockSize == 0, there is no more data to be read.
      while curShift < 16 && subblockSize != 0 do
        cur |= getUByte() << curShift
        curShift += 8

        if subblockSize == 1 then
          // Never let it get to 0 to hold logic above.
          subblockSize = getUByte() // Next subblock
        else
          subblockSize -= 1
      end while

      // TODO(deanm): We should never really get here, we should have received and EOI.
      if curShift < curCodeSize then
        break = true
      else
        var code = cur & codeMask
        cur >>= curCodeSize
        curShift -= curCodeSize

        /* TODO(deanm): Maybe should check that the first code was a clear code,
         * at least this is what you're supposed to do.  But actually our encoder
         * now doesn't emit a clear code first anyway.
         **/
        code match
          case `clearCode` =>
            /* We don't actually have to clear the table.  This could be a good idea
             * for greater error checking, but we don't really do any anyway.  We
             * will just track it with next_code and overwrite old entries.
             */

            nextCode = eoiCode + 1
            curCodeSize = minCodeSize + 1
            codeMask = (1 << curCodeSize) - 1

            // Don't update prev_code ?
            prevCode = Int.MinValue

          case `eoiCode` =>
            break = true

          case _ =>
            /* We have a similar situation as the decoder, where we want to store
             * variable length entries (code table entries), but we want to do in a
             * faster manner than an array of arrays.  The code below stores sort of a
             * linked list within the code table, and then "chases" through it to
             * construct the dictionary entries.  When a new entry is created, just the
             * last byte is stored, and the rest (prefix) of the entry is only
             * referenced by its table entry.  Then the code chases through the
             * prefixes until it reaches a single byte code.  We have to chase twice,
             * first to compute the length, and then to actually copy the data to the
             * output (backwards, since we know the length).  The alternative would be
             * storing something in an intermediate stack, but that doesn't make any
             * more sense.  I implemented an approach where it also stored the length
             * in the code table, although it's a bit tricky because you run out of
             * bits (12 + 12 + 8), but I didn't measure much improvements (the table
             * entries are generally not the long).  Even when I created benchmarks for
             * very long table entries the complexity did not seem worth it.
             * The code table stores the prefix entry in 12 bits and then the suffix
             * byte in 8 bits, so each entry is 20 bits.
             */

            var chaseCode = if code < nextCode then code else prevCode

            // Chase what we will output, either {CODE} or {CODE-1}
            var chaseLength = 0
            var chase = chaseCode
            while chase > clearCode do
              chase = codeTable(chase) >> 8
              chaseLength += 1

            val k = chase

            var opEnd = op + chaseLength + (if chaseCode != code then 1 else 0)
            if opEnd > outputLength then
              println("Warning, gif stream longer than expected.")
              return

            // Already have the first byte from the chase, might as well write it fast.
            output(op) = k.toByte
            op += 1

            op += chaseLength
            var b = op // Track pointer, writing backwards.

            if chaseCode != code then // The case of emitting {CODE-1} + k.
              output(op) = k.toByte
              op += 1

            chase = chaseCode
            while chaseLength != 0 do
              chaseLength -= 1
              chase = codeTable(chase)
              b -= 1 // Write backwards
              output(b) = chase.toByte
              chase >>= 8 // Pull down to the prefix code
            end while

            if prevCode != Int.MinValue && nextCode < 4096 then
              codeTable(nextCode) = (prevCode << 8) | k
              nextCode += 1
              // TODO(deanm): Figure out this clearing vs code growth logic better.  I
              // have an feeling that it should just happen somewhere else, for now it
              // is awkward between when we grow past the max and then hit a clear code.
              // For now just check if we hit the max 12-bits (then a clear code should
              // follow, also of course encoded in 12-bits).
              if nextCode >= codeMask + 1 && curCodeSize < 12 then
                curCodeSize += 1
                codeMask = (codeMask << 1) | 1
            end if

            prevCode = code
      end if
    end while

    if op != outputLength then
      println("Warning, gif stream shorter than expected.")
  end gifReaderLZWOutputIndexStream
end GIFReader

object GIFReader:
  private final val NoTransparentColor = 256

  final class Frame(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val paletteOffset: Int,
    val paletteSize: Int,
    val dataOffset: Int,
    val dataLength: Int,
    val transparentIndex: Int,
    val interlaced: Boolean,
    val delay: Int,
    val disposal: Int,
  )
end GIFReader
