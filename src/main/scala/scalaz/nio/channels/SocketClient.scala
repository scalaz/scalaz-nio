package scalaz.nio.channels

import java.net.SocketAddress
import scalaz.nio.ByteBuffer
import scalaz.zio.{ IO, Managed }

class SocketClient private (channel: AsynchronousSocketChannel) {

  def write(bytes: Array[Byte]): IO[Exception, Unit] =
    for {
      buffer <- ByteBuffer(bytes)
      _      <- channel.write(buffer)
    } yield ()

  def read(numBytes: Int): IO[Exception, Array[Byte]] =
    for {
      buffer <- ByteBuffer(numBytes)
      _      <- channel.read(buffer)
      array  <- buffer.array
    } yield array

  def close: IO[Exception, Unit] = channel.close

}

object SocketClient {

  def apply(socketAddress: SocketAddress): Managed[Exception, SocketClient] =
    Managed(for {
      channel <- AsynchronousSocketChannel()
      _       <- channel.connect(socketAddress)
    } yield new SocketClient(channel))(_.close.attempt.void)

}
