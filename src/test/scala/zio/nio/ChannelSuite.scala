package zio.nio

import zio.nio.channels.{ AsynchronousServerSocketChannel, AsynchronousSocketChannel }
import zio.{ Chunk, DefaultRuntime, IO, ZIO }
import testz.{ Harness, assert }

object ChannelSuite extends DefaultRuntime {

  def tests[T](harness: Harness[T]): T = {
    import harness._
    section(
      test("read/write") { () =>
        val inetAddress = InetAddress.localHost
          .flatMap(iAddr => SocketAddress.inetSocketAddress(iAddr, 13370))

        def echoServer: IO[Exception, Unit] =
          for {
            address <- inetAddress
            sink    <- Buffer.byte(3)
            server  <- AsynchronousServerSocketChannel()
            _       <- server.bind(address)
            worker  <- server.accept
            _       <- worker.readBuffer(sink)
            _       <- sink.flip
            _       <- worker.writeBuffer(sink)
            _       <- worker.close
            _       <- server.close
          } yield ()

        def echoClient: IO[Exception, Boolean] =
          for {
            address  <- inetAddress
            src      <- Buffer.byte(3)
            client   <- AsynchronousSocketChannel()
            _        <- client.connect(address)
            sent     <- src.array
            _        = sent.update(0, 1)
            _        <- client.writeBuffer(src)
            _        <- src.flip
            _        <- client.readBuffer(src)
            received <- src.array
            _        <- client.close
          } yield sent.sameElements(received)

        val testProgram: IO[Exception, Boolean] = for {
          serverFiber <- echoServer.fork
          clientFiber <- echoClient.fork
          _           <- serverFiber.join
          same        <- clientFiber.join
        } yield same

        assert(unsafeRun(testProgram))
      },
      test("read should fail when connection close") { () =>
        val inetAddress = InetAddress.localHost
          .flatMap(iAddr => SocketAddress.inetSocketAddress(iAddr, 13370))

        def server: IO[Exception, Boolean] = {
          for {
            address <- inetAddress
            server  <- AsynchronousServerSocketChannel()
            _       <- server.bind(address)
            worker  <- server.accept
            _       <- worker.read(3)
            _       <- worker.read(3)
            _       <- worker.close
            _       <- server.close
          } yield false
        }.catchSome {
          case ex: java.io.IOException if ex.getMessage == "Connection reset by peer" =>
            ZIO.succeed(true)
        }

        def client: IO[Exception, Unit] =
          for {
            address <- inetAddress
            client  <- AsynchronousSocketChannel()
            _       <- client.connect(address)
            _       = client.write(Chunk.fromArray(Array[Byte](1, 1, 1)))
            _       <- client.close
          } yield ()

        val testProgram: IO[Exception, Boolean] = for {
          serverFiber <- server.fork
          clientFiber <- client.fork
          same        <- serverFiber.join
          _           <- clientFiber.join
        } yield same

        assert(unsafeRun(testProgram))
      }
    )

  }
}
