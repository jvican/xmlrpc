package xmlrpc

import akka.actor.ActorRefFactory
import akka.util.Timeout
import spray.client.pipelining._
import spray.http.MediaTypes._
import spray.http.{HttpEntity, Uri}
import xmlrpc.protocol.{XmlrpcProtocol, Datatype, Deserializer}
import Deserializer.Deserialized

import scala.concurrent.{ExecutionContext, Future}
import scala.xml.NodeSeq

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

  def invokeMethod[P: Datatype, R: Datatype](name: String, parameters: P)(implicit xmlrpcServer: Uri,
                                                                            rf: ActorRefFactory,
                                                                            ec: ExecutionContext,
                                                                            fc: Timeout): Future[Deserialized[R]] =
    invokeMethod(name, Some(parameters))

  def invokeMethod[P: Datatype, R: Datatype](name: String, parameters: Option[P] = None)
                                              (implicit xmlrpcServer: Uri,
                                               rf: ActorRefFactory,
                                               ec: ExecutionContext,
                                               fc: Timeout): Future[Deserialized[R]] = {

    val request: NodeSeq = writeXmlRequest(name, parameters)

    import spray.httpx.unmarshalling.BasicUnmarshallers.NodeSeqUnmarshaller

    val pipeline = sendReceive ~> unmarshal[NodeSeq]

    // As scala-xml doesn't support xml tags, because it is a reserved keyword, xlm is converted
    // to a String and then the standard xml header is added
    val requestWithHeader: String = <?xml version="1.0"?> + request.toString

    pipeline(Post(xmlrpcServer, HttpEntity(`text/xml`, requestWithHeader))).map(xml => readXmlResponse[R](xml))
  }
}
