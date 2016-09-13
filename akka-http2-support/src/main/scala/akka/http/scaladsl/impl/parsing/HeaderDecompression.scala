package akka.http.scaladsl.impl.parsing

import akka.http.scaladsl.model.{ HttpHeader, HttpMethod, HttpMethods }
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model2.HeadersFrame
import akka.stream.stage._
import akka.stream.{ Attributes, FlowShape, Inlet, Outlet }
import com.twitter.hpack.HeaderListener

final class HeaderDecompression extends GraphStage[FlowShape[HeadersFrame, HttpHeader]] {
  import HeaderDecompression._

  // FIXME Make configurable
  final val maxHeaderSize = 4096
  final val maxHeaderTableSize = 4096
  final val name = "name".getBytes()
  final val value = "value".getBytes()
  final val sensitive = false

  private final val ColonByte = ':'.toByte

  val in = Inlet[HeadersFrame]("HeaderDecompression.in")
  val out = Outlet[HttpHeader]("HeaderDecompression.out")
  override val shape = FlowShape.of(in, out)

  // format: OFF
  override def createLogic(inheritedAttributes: Attributes) =
    new GraphStageLogic(shape)
      with InHandler with OutHandler
      with HeaderListener {
      // format: ON

      val decoder = new com.twitter.hpack.Decoder(maxHeaderSize, maxHeaderTableSize)

      override def onPush(): Unit = {
        val headersFrame = grab(in)

        val is = ByteStringInputStream(headersFrame.headerBlockFragment)
        decoder.decode(is, this) // this: HeaderListener (invoked synchronously)
      }

      override def onPull(): Unit =
        pull(in)

      setHandlers(in, out, this)

      override def onUpstreamFinish(): Unit = {
        completeStage()
      }

      override def addHeader(name: Array[Byte], value: Array[Byte], sensitive: Boolean): Unit = {
        val nameString = new String(name) // FIXME wasteful :-(
        println("name.toList = " + nameString)

        val header: HttpHeader =
          if (name.head == ColonByte)
            // FIXME this must be optimised
            nameString match {
              //              case ":method" ⇒ HttpMethods.getForKeyCaseInsensitive(new String(value)).get // TODO how should we handle this?
              case ":path" ⇒ RawHeader(nameString, new String(value))
              case unknown ⇒
                throw new Exception(s": prefixed header should be emitted well-typed! Was: '${new String(unknown)}'. This is a bug.")
            }
          else RawHeader(nameString, new String(value))

        push(out, header)
      }
    }

  // FIXME actually convert to the real typed model
  // FIXME carry-on sesitive flag?
  def convertToModelledHeader(data: HeaderData): HttpHeader =
    RawHeader(new String(data.name), new String(data.value))

}

object HeaderDecompression {
  final case class HeaderData(name: Array[Byte], value: Array[Byte], sensitive: Boolean) {
    override lazy val toString =
      if (sensitive) s"${getClass.getName}(${new String(name)}, <sensitive-value>)"
      else s"${getClass.getName}(${new String(name)}, ${new String(value)})"
  }
}
