package scalaz.nio.channels

import java.io.IOException
import java.nio.channels.{ ClosedSelectorException, Selector => JSelector }

import scalaz.nio.channels.spi.SelectorProvider
import scalaz.nio.io._
import scalaz.zio.{ IO, JustExceptions, UIO }
import scalaz.zio.duration.Duration

import scala.collection.JavaConverters

class Selector(private[nio] val selector: JSelector) {

  import Selector._

  final val isOpen: UIO[Boolean] = IO.effectTotal(selector.isOpen)

  final val provider: UIO[SelectorProvider] =
    IO.effectTotal(selector.provider()).map(new SelectorProvider(_))

  final val keys: IO[ClosedSelectorException, Set[SelectionKey]] =
    IO.effect(selector.keys()).refineOrDie(JustClosedSelectorException).map { keys =>
      JavaConverters.asScalaSet(keys).toSet.map(new SelectionKey(_))
    }

  final val selectedKeys: IO[ClosedSelectorException, Set[SelectionKey]] =
    IO.effect(selector.selectedKeys()).refineOrDie(JustClosedSelectorException).map { keys =>
      JavaConverters.asScalaSet(keys).toSet.map(new SelectionKey(_))
    }

  final def removeKey(key: SelectionKey): UIO[Unit] =
    IO.effectTotal(selector.selectedKeys().remove(key.selectionKey)).void

  /**
   * Can throw IOException and ClosedSelectorException.
   */
  final val selectNow: IO[Exception, Int] =
    IO.effect(selector.selectNow()).refineOrDie(JustExceptions)

  /**
   * Can throw IOException and ClosedSelectorException.
   */
  final def select(timeout: Duration): IO[Exception, Int] =
    IO.effect(selector.select(timeout.toMillis)).refineOrDie(JustExceptions)

  /**
   * Can throw IOException and ClosedSelectorException.
   */
  final val select: IO[Exception, Int] =
    IO.effect(selector.select()).refineOrDie(JustExceptions)

  final val wakeup: IO[Nothing, Selector] =
    IO.effectTotal(selector.wakeup()).map(new Selector(_))

  final val close: IO[IOException, Unit] =
    IO.effect(selector.close()).refineOrDie(JustIOException).void
}

object Selector {

  val JustClosedSelectorException: PartialFunction[Throwable, ClosedSelectorException] = {
    case e: ClosedSelectorException => e
  }

  final val make: IO[IOException, Selector] =
    IO.effect(new Selector(JSelector.open())).refineOrDie(JustIOException)

}
