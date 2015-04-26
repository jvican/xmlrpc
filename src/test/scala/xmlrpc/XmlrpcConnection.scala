package xmlrpc

import akka.actor.ActorSystem
import akka.util.Timeout
import org.scalatest.FunSpec
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import xmlrpc.protocol.XmlrpcProtocol

import scala.concurrent.duration._
import scalaz.{Success, Failure}

class XmlrpcConnection extends FunSpec with ScalaFutures {
  // Xmlrpc imports
  import Xmlrpc._
  import XmlrpcProtocol._

  // Scalatest setup
  implicit val default: PatienceConfig = PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

  // Xmrpc setup, server is up but it is not mine, found on Internet
  implicit val testServer = XmlrpcServer("http://betty.userland.com/RPC2")

  // Spray setup
  implicit val system = ActorSystem()
  implicit val timeout = Timeout(5 seconds)
  import system.dispatcher

  describe("The connection with a XML-RPC server") {
    it("should invoke the test method successfully in the server") {
      val invocation = invokeMethod[Int, String]("examples.getStateName", 41).underlying
      val responseMessage = "South Dakota"

      whenReady(invocation) {
        case Success(value) => assertResult(responseMessage) {value}
        case Failure(errors) => fail("Errors when deserializing\n" + errors)
      }
    }
  }
}
