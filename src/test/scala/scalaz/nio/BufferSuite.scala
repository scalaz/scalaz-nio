package scalaz.nio

import java.nio.{ ByteBuffer => JByteBuffer }

import scalaz.zio.{ IO, RTS }
import testz.{ Harness, assert }

object BufferSuite extends RTS {

  def tests[T](harness: Harness[T]): T = {
    import harness._

    val initialCapacity = 10
    val newLimit        = 3
    section(
      test("capacity") { () =>
        val testProgram: IO[Exception, Boolean] = for {
          bb <- Buffer.byte(initialCapacity)
          c1 <- bb.capacity
          c2 <- IO.sync {
                 JByteBuffer.allocate(initialCapacity).capacity
               }
        } yield c1 == c2
        assert(unsafeRun(testProgram))
      }, {

        def allocate = Buffer.byte(initialCapacity)

        namedSection("allocate")(
          test("capacity initialized") { () =>
            val capacity = unsafeRun(allocate.flatMap(b => b.capacity))
            assert(capacity == initialCapacity)
          },
          test("position is 0") { () =>
            val position = unsafeRun(allocate.flatMap(b => b.position))
            assert(position == 0)
          },
          test("limit is capacity") { () =>
            val limit = unsafeRun(allocate.flatMap(b => b.limit))
            assert(limit == initialCapacity)
          }
        )
      },
      namedSection("position") {
        val newPosition = 3

        def position =
          for {
            b <- Buffer.byte(initialCapacity)
            _ <- b.position(newPosition)
          } yield b

        test("position set") { () =>
          val actual = unsafeRun(position.flatMap(b => b.position))
          assert(actual == newPosition)
        }
      },
      namedSection("limit")(
        test("limit set") { () =>
          val limit = for {
            b        <- Buffer.byte(initialCapacity)
            _        <- b.limit(newLimit)
            newLimit <- b.limit
          } yield newLimit

          assert(unsafeRun(limit) == newLimit)
        },
        test("position reset") { () =>
          val positionReset = for {
            b        <- Buffer.byte(initialCapacity)
            _        <- b.position(newLimit + 1)
            _        <- b.limit(newLimit)
            position <- b.position
          } yield position

          assert(unsafeRun(positionReset) == newLimit)
        }
      ),
      test("reset to marked position") { () =>
        val markedPosition = for {
          b           <- Buffer.byte(initialCapacity)
          _           <- b.position(1)
          _           <- b.mark
          _           <- b.position(2)
          _           <- b.reset
          newPosition <- b.position
        } yield newPosition

        assert(unsafeRun(markedPosition) == 1)
      }, {
        def clear =
          for {
            b <- Buffer.byte(initialCapacity)
            _ <- b.position(1)
            _ <- b.mark
            _ <- b.clear
          } yield b

        namedSection("clear")(
          test("position is 0") { () =>
            val position = unsafeRun(clear.flatMap(b => b.position))
            assert(position == 0)
          },
          test("limit is capacity") { () =>
            val limit = unsafeRun(clear.flatMap(b => b.limit))
            assert(limit == initialCapacity)
          }
        )
      }, {
        def flip =
          for {
            b <- Buffer.byte(initialCapacity)
            _ <- b.position(1)
            _ <- b.flip
          } yield b

        namedSection("flip")(
          test("limit is position") { () =>
            val limit = unsafeRun(flip.flatMap(b => b.limit))
            assert(limit == 1)
          },
          test("position is 0") { () =>
            val position = unsafeRun(flip.flatMap(b => b.position))
            assert(position == 0)
          }
        )
      },
      test("rewind sets position to 0") { () =>
        val rewindedPosition = for {
          b           <- Buffer.byte(initialCapacity)
          _           <- b.position(1)
          _           <- b.rewind
          newPosition <- b.position
        } yield newPosition
        assert(unsafeRun(rewindedPosition) == 0)
      },
      test("heap buffers a backed by an array") { () =>
        val hasArray = for {
          b        <- Buffer.byte(initialCapacity)
          hasArray <- b.hasArray
        } yield hasArray
        assert(unsafeRun(hasArray))
      }
    )
  }

}

object ByteBufferSuite extends RTS {
  def tests[T](harness: Harness[T]): T = {
    import harness._

    section(
      test("asIntBuffer") { () =>
        val intBuffer = ByteBuffer(3).flatMap(_.asIntBuffer)

        assert(unsafeRun(intBuffer).isInstanceOf[IntBuffer])
      }
    )
  }
}

object IntBufferSuite extends RTS {
  def tests[T](harness: Harness[T]): T = {
    import harness._

    def initialValues = Array(1, 2, 3)

    section(
      test("apply") { () =>
        val apply = IntBuffer(3).flatMap(_.array)

        assert(unsafeRun(apply) sameElements Array(0, 0, 0))
      },
      namedSection("wrap")(
        test("backed by an array") { () =>

          val wrap =
            for {
              intBuffer <- IntBuffer.wrap(initialValues)
              array     <- intBuffer.array
            } yield array

          assert(unsafeRun(wrap) sameElements initialValues)
        }, {

          val wrap = IntBuffer.wrap(initialValues, 1, 2)

          namedSection("backed by array with offset and length")(
            test("array") { () =>
              assert(unsafeRun(wrap.flatMap(_.array)) sameElements initialValues)
            },
            test("position") { () =>
              assert(unsafeRun(wrap.flatMap(_.position)) == 1)
            },
            test("remaining") { () =>
              assert(unsafeRun(wrap.flatMap(_.remaining)) == 2)
            }
          )
        }
      ),

      namedSection("get")(
        test("at current position") { () =>

          val get = IntBuffer.wrap(initialValues, 1, 2).flatMap(_.get)

          assert(unsafeRun(get) == 2)
        },
        test("at index") { () =>

          val get = IntBuffer.wrap(initialValues).flatMap(_.get(1))

          assert(unsafeRun(get) == 2)
        },
        test("should update position") { () =>

          val position =
            for {
              buffer <- IntBuffer.wrap(initialValues)
              _      <- buffer.get
              pos    <- buffer.position
            } yield pos

          assert(unsafeRun(position) == 1)
        }
      ),

      namedSection("bulkGet")(
        test("entire buffer") { () =>

          val bulkGet = IntBuffer.wrap(initialValues).flatMap(_.bulkGet)

          assert(unsafeRun(bulkGet) sameElements initialValues)
        },
        test("offset of buffer") { () =>

          val bulkGet = IntBuffer.wrap(initialValues).flatMap(_.bulkGet(3, 2))

          assert(unsafeRun(bulkGet) sameElements Array(0, 0, 0, 1, 2))
        }
      ),

      namedSection("put")(
        test("at current position") { () =>

          val put = for {
            buffer <- IntBuffer.wrap(initialValues)
            _      <- buffer.put(0)
            array  <- buffer.array
          } yield array

          assert(unsafeRun(put) sameElements Array(0, 2, 3))
        },
        test("at index") { () =>

          val put = for {
            buffer <- IntBuffer.wrap(initialValues)
            _      <- buffer.put(1, 0)
            array  <- buffer.array
          } yield array

          assert(unsafeRun(put) sameElements Array(1, 0, 3))
        },
        test("contents of another IntBuffer") { () =>

          val put = for {
            buffer1 <- IntBuffer.wrap(initialValues)
            buffer2 <- IntBuffer(3)
            _       <- buffer2.put(buffer1)
            array   <- buffer2.array
          } yield array

          assert(unsafeRun(put) sameElements initialValues)
        },
        test("contents of an array") { () =>

          val put = for {
            buffer <- IntBuffer(3)
            _      <- buffer.put(initialValues)
            array  <- buffer.array
          } yield array

          assert(unsafeRun(put) sameElements initialValues)
        },
        test("offset contents of an array") { () =>

          val put = for {
            buffer <- IntBuffer(2)
            _      <- buffer.put(initialValues, 1, 2)
            array  <- buffer.array
          } yield array

          assert(unsafeRun(put) sameElements Array(2, 3))
        }
      ),

      namedSection("slice")(
        test("creates a new IntBuffer") { () =>

          val slice = for {
            buffer1 <- IntBuffer.wrap(initialValues)
            _       <- buffer1.get
            buffer2 <- buffer1.slice
          } yield buffer2

          assert(unsafeRun(slice.flatMap(_.array)) sameElements initialValues)
          assert(unsafeRun(slice.flatMap(_.position)) == 0)
        }
      ),

      namedSection("duplicate")(
        test("creates a new IntBuffer") { () =>

          val duplicate = for {
            buffer1 <- IntBuffer.wrap(initialValues)
            _       <- buffer1.get
            buffer2 <- buffer1.duplicate
          } yield buffer2

          assert(unsafeRun(duplicate.flatMap(_.array)) sameElements initialValues)
          assert(unsafeRun(duplicate.flatMap(_.position)) == 1)
        }
      ),

      namedSection("asReadOnlyBuffer")(
        test("creates a read only IntBuffer") { () =>

          val asReadOnlyBuffer = for {
            buffer1  <- IntBuffer.wrap(initialValues)
            buffer2  <- buffer1.asReadOnlyBuffer
            readOnly <- buffer2.isReadOnly
          } yield readOnly

          assert(unsafeRun(asReadOnlyBuffer))
        }
      )
    )
  }
}