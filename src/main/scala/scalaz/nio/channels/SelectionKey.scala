package scalaz.nio.channels

import java.nio.channels.{
  CancelledKeyException,
  SelectableChannel => JSelectableChannel,
  SelectionKey => JSelectionKey
}

import scalaz.zio.{ IO, UIO }

object SelectionKey {

  val JustCancelledKeyException: PartialFunction[Throwable, CancelledKeyException] = {
    case e: CancelledKeyException => e
  }

}

class SelectionKey(private[nio] val selectionKey: JSelectionKey) {

  import SelectionKey._

  final val channel: UIO[JSelectableChannel] =
    IO.effectTotal(selectionKey.channel())

  final val selector: UIO[Selector] =
    IO.effectTotal(selectionKey.selector()).map(new Selector(_))

  final val isValid: UIO[Boolean] =
    IO.effectTotal(selectionKey.isValid)

  final val cancel: UIO[Unit] =
    IO.effectTotal(selectionKey.cancel())

  final val interestOps: IO[CancelledKeyException, Int] =
    IO.effect(selectionKey.interestOps()).refineOrDie(JustCancelledKeyException)

  final def interestOps(ops: Int): IO[CancelledKeyException, SelectionKey] =
    IO.effect(selectionKey.interestOps(ops))
      .map(new SelectionKey(_))
      .refineOrDie(JustCancelledKeyException)

  final val readyOps: IO[CancelledKeyException, Int] =
    IO.effect(selectionKey.readyOps()).refineOrDie(JustCancelledKeyException)

  final def isReadable: IO[CancelledKeyException, Boolean] =
    IO.effect(selectionKey.isReadable()).refineOrDie(JustCancelledKeyException)

  final def isWritable: IO[CancelledKeyException, Boolean] =
    IO.effect(selectionKey.isWritable()).refineOrDie(JustCancelledKeyException)

  final def isConnectable: IO[CancelledKeyException, Boolean] =
    IO.effect(selectionKey.isConnectable()).refineOrDie(JustCancelledKeyException)

  final def isAcceptable: IO[CancelledKeyException, Boolean] =
    IO.effect(selectionKey.isAcceptable()).refineOrDie(JustCancelledKeyException)

  final def attach(ob: Option[AnyRef]): UIO[Option[AnyRef]] =
    IO.effectTotal(Option(selectionKey.attach(ob.orNull)))

  final def attach(ob: AnyRef): UIO[AnyRef] =
    IO.effectTotal(selectionKey.attach(ob))

  final val detach: UIO[Unit] =
    IO.effectTotal(selectionKey.attach(null)).map(_ => ())

  final val attachment: UIO[Option[AnyRef]] =
    IO.effectTotal(selectionKey.attachment()).map(Option(_))

}
