package scalaz.nio.channels

import scalaz.nio.{ByteBuffer, InetSocketAddress, SocketAddress}
import scalaz.zio.{IO, Managed}

class SocketClient private (channel: AsynchronousSocketChannel) {

  def write(bytes: Array[Byte]): IO[Exception, Unit] =
    for {
      buffer <- ByteBuffer(bytes)
      _ <- channel.write(buffer)
    } yield ()

  def read(numBytes: Int): IO[Exception, Array[Byte]] =
    for {
      buffer <- ByteBuffer(numBytes)
      _ <- channel.read(buffer)
      array <- buffer.array
    } yield array

  def close: IO[Exception, Unit] = channel.close

}

object SocketClient {

  def apply(host: String, port: Int): Managed[Exception, SocketClient] =
    Managed(for {
      address <- SocketAddress.inetSocketAddress(host, port)
      channel <- AsynchronousSocketChannel()
      _ <- channel.connect(address)
    } yield new SocketClient(channel))(_.close.attempt.void)

  def apply(address: InetSocketAddress): Managed[Exception, SocketClient] =
    Managed(for {
      channel <- AsynchronousSocketChannel()
      _ <- channel.connect(address)
    } yield new SocketClient(channel))(_.close.attempt.void)

}
