package xmlrpc.protocol

import xmlrpc.protocol.Deserializer.{DeserializationError, Fault, Deserialized}

import scala.concurrent.Future
import scala.language.{implicitConversions, postfixOps}
import scala.util.Try
import scala.xml.Utility.trim
import scala.xml._
import scalaz.Scalaz._
import scalaz.{NonEmptyList, Validation}

trait Deserializer[T] {
  import Deserializer.Deserialized
  def deserialize(from: NodeSeq): Deserialized[T]
}

trait AnyError { val friendlyMessage: String }

object Deserializer {
  // This is, actually, validationNel
  type Deserialized[T] = Validation[AnyErrors, T]

  type AnyErrors = NonEmptyList[AnyError]

  trait XmlrpcError extends AnyError

  case class DeserializationError(reason: String, throwable: Option[Throwable] = None) extends XmlrpcError {
    override val friendlyMessage: String = s"A deserialization error has occurred: \n$reason\n" +
      s"Exception: \n\t${throwable.getOrElse("Unknown")}"
  }

  // This is a fault from the server, it means an error in the server side with information about why
  case class Fault(code: Int, reason: String) extends XmlrpcError {
    override val friendlyMessage = s"Fault response from the server: $reason with code $code\n"
  }

  implicit class StringToError(reason: String) {
    val toError: DeserializationError = DeserializationError(reason)
  }

  implicit class ToFailures(e: AnyError) extends AnyRef {
    def failures[A] = Validation.failureNel[AnyError, A](e)
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
            Fault(intValue.text.toInt, stringValue.text).failures

          case _ => s"fault structure with faultCode and faultString not found".toError.failures
        }

      case <methodResponse>{params}</methodResponse> if params.label == "params" =>
        Try {
          fromXmlrpc[T](params \ "param")
        } recover { case t =>
          DeserializationError(
            "This is an unexpected error. It may be because the type of response is not the specified",
            Some(t)
          ).failures
        } get

      case <error/> => s"methodResponse tag expected in $xml".toError.failures

      case _ => s"Body of response with params or fault couldn't be parsed. Expected methodResponse structure.\n$xml".toError.failures
    }
  
  def writeXmlRequest[P: Datatype](methodName: String, parameter: P): Node =
      <methodCall>
        <methodName>{methodName}</methodName>
        <params>
          { for { param <- toXmlrpc[P](parameter)} yield param.inParam }
        </params>
      </methodCall>
}

object XmlrpcProtocol extends Protocol
                          with BasicTypes
                          with CollectionTypes
                          with ScalaTypes
                          with GenericTypes
                          with ShapelessTypes
