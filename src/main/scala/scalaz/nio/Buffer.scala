package scalaz.nio

import scalaz.zio.IO
//import scalaz.Scalaz._

import java.nio.{ Buffer => JBuffer, ByteBuffer => JByteBuffer, IntBuffer => JIntBuffer }

import scala.reflect.ClassTag
//import scala.{Array => SArray}

//case class Array[A: ClassTag](private val array: SArray[A]) {
//  final def length = array.length

// Expose all methods in IO
//}

@specialized // See if Specialized will work on return values, e.g. `get`
abstract class Buffer[A: ClassTag, B <: JBuffer] private[nio] (private[nio] val buffer: B) {
  final def capacity: IO[Nothing, Int] = IO.now(buffer.capacity)

  final def position: IO[Nothing, Int] = IO.now(buffer.position)

  final def position(newPosition: Int): IO[Exception, Unit] =
    IO.syncException(buffer.position(newPosition)).void

  final def limit: IO[Nothing, Int] = IO.now(buffer.limit)

  final def remaining: IO[Nothing, Int] = IO.now(buffer.remaining)

  final def hasRemaining: IO[Nothing, Boolean] = IO.now(buffer.hasRemaining)

  final def limit(newLimit: Int): IO[Exception, Unit] =
    IO.syncException(buffer.limit(newLimit)).void

  final def mark: IO[Nothing, Unit] = IO.sync(buffer.mark()).void

  final def reset: IO[Nothing, Unit] = IO.sync(buffer.reset()).void

  final def clear: IO[Nothing, Unit] = IO.sync(buffer.clear()).void

  final def flip: IO[Nothing, Unit] = IO.sync(buffer.flip()).void

  final def rewind: IO[Nothing, Unit] = IO.sync(buffer.rewind()).void

  final def isReadOnly: IO[Nothing, Boolean] = IO.now(buffer.isReadOnly)

  def array: IO[Exception, Array[A]]

  final def hasArray: IO[Nothing, Boolean] = IO.now(buffer.hasArray)
  final def arrayOffset: IO[Nothing, Int]  = IO.now(buffer.arrayOffset)
  final def isDirect: IO[Nothing, Boolean] = IO.now(buffer.isDirect)

}

class ByteBuffer private (val byteBuffer: JByteBuffer)
    extends Buffer[Byte, JByteBuffer](byteBuffer) {

  def array: IO[Exception, Array[Byte]] = IO.syncException(byteBuffer.array())

  def asIntBuffer: IO[Exception, IntBuffer] =
    IO.syncException(byteBuffer.asIntBuffer()).map(new IntBuffer(_))
}

object ByteBuffer {

  def apply(capacity: Int): IO[Exception, ByteBuffer] =
    IO.syncException(JByteBuffer.allocate(capacity)).map(new ByteBuffer(_))
}

object Buffer {
  def byte(capacity: Int) = ByteBuffer(capacity)

  def char(capacity: Int) = CharBuffer(capacity)

  private class CharBuffer private (private val charBuffer: JByteBuffer)
      extends Buffer[Char, JByteBuffer](charBuffer) {

    def array: IO[Exception, Array[Char]] =
      IO.syncException(charBuffer.array().asInstanceOf[Array[Char]])
  }

  private object CharBuffer {

    def apply(capacity: Int): Buffer[Char, JByteBuffer] =
      new CharBuffer(JByteBuffer.allocate(capacity))
  }

}

class IntBuffer private[nio] (private val intBuffer: JIntBuffer)
    extends Buffer[Int, JIntBuffer](intBuffer) {

  val array: IO[Exception, Array[Int]] = IO.syncException(intBuffer.array())

  val get: IO[Exception, Int] = IO.syncException(intBuffer.get())
  def get(index: Int): IO[Exception, Int] = IO.syncException(intBuffer.get(index))

  val bulkGet: IO[Exception, Array[Int]] =
    IO.syncException {
      val array = new Array[Int](intBuffer.limit())
      intBuffer.get(array)
      array
    }

  def bulkGet(offset: Int, length: Int): IO[Exception, Array[Int]] =
    IO.syncException {
      val array = new Array[Int](offset + length)
      intBuffer.get(array, offset, length)
      array
    }

  def put(i: Int): IO[Exception, IntBuffer] =
    IO.syncException {
      intBuffer.put(i)
      this
    }

  def put(index: Int, i: Int): IO[Exception, IntBuffer] =
    IO.syncException {
      intBuffer.put(index, i)
      this
    }

  def put(src: IntBuffer): IO[Exception, IntBuffer] =
    IO.syncException {
      intBuffer.put(src.intBuffer)
      this
    }

  def put(src: Array[Int], offset: Int, length: Int): IO[Exception, IntBuffer] =
    IO.syncException {
      intBuffer.put(src, offset, length)
      this
    }

  def put(src: Array[Int]): IO[Exception, IntBuffer] =
    IO.syncException {
      intBuffer.put(src)
      this
    }

  val slice: IO[Nothing, IntBuffer] = IO.sync(intBuffer.slice()).map(new IntBuffer(_))
  val duplicate: IO[Nothing, IntBuffer] = IO.sync(intBuffer.duplicate()).map(new IntBuffer(_))
  val asReadOnlyBuffer: IO[Nothing, IntBuffer] = IO.sync(intBuffer.asReadOnlyBuffer()).map(new IntBuffer(_))

}

object IntBuffer {

  def apply(capacity: Int): IO[Exception, IntBuffer] =
    IO.syncException(JIntBuffer.allocate(capacity)).map(new IntBuffer(_))

  def wrap(array: Array[Int], offset: Int, length: Int): IO[Exception, IntBuffer] =
    IO.syncException(JIntBuffer.wrap(array, offset, length)).map(new IntBuffer(_))

  def wrap(array: Array[Int]): IO[Exception, IntBuffer] =
    IO.syncException(JIntBuffer.wrap(array)).map(new IntBuffer(_))
}
