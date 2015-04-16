package xmlrpc


import xmlrpc.Deserializer.{Deserialized, Fault}

import scala.language.{implicitConversions, postfixOps}
import scala.xml.Utility.trim
import scala.xml._
import scalaz.Scalaz._
import scalaz.ValidationNel

trait Deserializer[T] {
  import Deserializer.Deserialized
  def deserialize(from: NodeSeq): Deserialized[T]
}

object Deserializer {
  type Deserialized[T] = ValidationNel[Error, T]

  trait Error { val friendlyMessage: String }

  case class DeserializationError(reason: String, exception: Option[Exception] = None) extends Error {
    override val friendlyMessage: String = s"A deserialization error has occurred: \n$reason\n" +
      s"Exception: \n\t${exception.getOrElse("Unknown")}"
  }

  // This is a fault from the server, it means an error in the server side with information about why
  case class Fault(code: Int, reason: String) extends Error {
    override val friendlyMessage = s"Fault response from the server: $reason with code $code\n"
  }

  implicit class StringToError(reason: String) {
    val toError: DeserializationError = DeserializationError(reason)
  }
}

trait Serializer[T] {
  def serialize(value: T): NodeSeq
}

trait Datatype[T] extends Serializer[T] with Deserializer[T]

trait Protocol extends DatetimeSpec with Helpers {
  import Deserializer.StringToError

  // TODO Write messages if implicits are not found

  def toXmlrpc[T](datatype: T)(implicit serializer: Serializer[T]): NodeSeq =
    serializer.serialize(datatype)

  def fromXmlrpc[T](value: NodeSeq)(implicit deserializer: Deserializer[T]): Deserialized[T] =
    deserializer.deserialize(value)

  def readXmlResponse[T: Datatype](xml: NodeSeq): Deserialized[T] =
    trim((xml \\ "methodResponse" headOption).getOrElse(<error/>)) match {

      case <methodResponse>{fault}</methodResponse> if fault.label == "fault" =>
        (fault \\ "int" headOption, fault \\ "string" headOption) match {
          case (Some(intValue), Some(stringValue)) =>
            Fault(intValue.text.toInt, stringValue.text).failureNel

          case _ => s"fault structure with faultCode and faultString not found".toError.failureNel
        }

      case <methodResponse>{params}</methodResponse> if params.label == "params" =>
        fromXmlrpc[T](params \ "param")

      case <error/> => s"methodResponse tag expected in $xml".toError.failureNel

      case _ => s"Body of response with params or fault couldn't be parsed. Expected methodResponse structure.\n$xml".toError.failureNel
    }
  
  trait MethodParameters {
    self: Product =>
  }

  def writeXmlRequest[P: Datatype](methodName: String, parameters: Option[P]): Node =
      <methodCall>
        <methodName>{methodName}</methodName>
        {if(parameters.isDefined)
          <params>
            { for { param <- toXmlrpc[P](parameters.get)} yield param.inParam }
          </params>}
      </methodCall>
}

object XmlrpcProtocol extends Protocol
                          with BasicDatatypes
                          with CollectionDatatypes
                          with ScalaTypes
