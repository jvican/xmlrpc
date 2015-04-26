package xmlrpc

import akka.actor.ActorRefFactory
import akka.util.Timeout
import spray.client.pipelining._
import spray.http.MediaTypes._
import spray.http.{HttpEntity, Uri}
import xmlrpc.protocol.{Datatype, XmlrpcProtocol}

import scala.concurrent.ExecutionContext
import scala.xml.NodeSeq
import scalaz.Scalaz._

/**
 * This is the client api to connect to the Xmlrpc server. A client can send any request
 * and he will receive a response. A request is a method call and a response is the result of
 * that method in the server or a fault.
 *
 * The configuration of the Server is a Uri, make sure you have this implicit in context
 * before calling invokeMethod.
 *
 */
object Xmlrpc {
  import XmlrpcProtocol._

  case class XmlrpcServer(fullAddress: String) {
    def uri: Uri = Uri(fullAddress)
  }

  def invokeMethod[P: Datatype, R: Datatype](name: String, parameter: P = Void)
                                              (implicit xmlrpcServer: XmlrpcServer,
                                               rf: ActorRefFactory,
                                               ec: ExecutionContext,
                                               fc: Timeout): XmlrpcResponse[R] = {

    val request: NodeSeq = writeXmlRequest(name, parameter)

    import XmlrpcResponse.SprayToXmlrpcResponse
    import spray.httpx.unmarshalling.BasicUnmarshallers.NodeSeqUnmarshaller

    val pipeline = sendReceive ~> unmarshal[NodeSeq]

    /** As scala-xml doesn't support xml tags (it is a reserved keyword), xml is converted
      * to a String and then the standard xml header is added */
    val requestWithHeader: String = """<?xml version="1.0"?>""" + request.toString

    try {
      pipeline(Post(xmlrpcServer.uri, HttpEntity(`text/xml`, requestWithHeader))).asXmlrpcResponse[R]
    } catch {
      case t: Throwable => XmlrpcResponse(ConnectionError("An exception has been thrown by Spray", Some(t)).failureNel)
    }
  }
}
