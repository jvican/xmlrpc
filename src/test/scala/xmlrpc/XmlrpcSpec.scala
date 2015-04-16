package xmlrpc

import java.util.Date

import org.scalatest.FunSpec
import xmlrpc.protocol.{XmlrpcProtocol, Datatype, Deserializer}
import Deserializer.Fault

import scala.xml.Node

class XmlrpcSpec extends FunSpec {
  import XmlrpcProtocol._
  import org.scalatest.StreamlinedXmlEquality._

  describe("Xmlrpc protocol") {
    it("should create a right xmlrpc request") {
      val created: Node = writeXmlRequest("examples.getStateName", Some(41))

      val request: Node =
        <methodCall>
          <methodName>{"examples.getStateName"}</methodName>
          <params>
            <param>
              <value><int>{41}</int></value>
            </param>
          </params>
        </methodCall>

      assert(created === request)
    }

    it("should serialize and deserialize case classes") {
      case class State(name: String, population: Double)
      val SouthDakota = State("South Dakota", 835.175)

      implicit val ExampleXmlrpc: Datatype[State] = asProduct2(State.apply)(State.unapply(_).get)

      assert(
        SouthDakota ===
          readXmlResponse[State](writeXmlRequest[State]("getStateInfo", Some(SouthDakota)).asResponse).toOption.get
      )
    }

    it("should serialize and deserialize arrays") {
      val primes: Seq[Int] = Vector(2, 3, 5, 7, 11, 13)

      assert(
        primes ===
          readXmlResponse[Seq[Int]](
            writeXmlRequest[Seq[Int]]("getSum", Some(primes)).asResponse
          ).toOption.get
      )
    }

    it("should serialize and deserialize structs") {
      val bounds = Map("lowerBound" -> 18, "upperBound" -> 139)

      assert(
        bounds ===
          readXmlResponse[Map[String, Int]](writeXmlRequest[Map[String, Int]]("setBounds", Some(bounds)).asResponse).toOption.get
      )
    }

    it("should deserialize a fault error from the server") {
      case class NonExistingResponse(a: Int)
      implicit val NonExistingResponseFormat: Datatype[NonExistingResponse] =
        asProduct1(NonExistingResponse.apply)(NonExistingResponse.unapply(_).get)

      assert(Fault(4, "Too many parameters.") ===
        readXmlResponse[NonExistingResponse](
          <methodResponse>
            <fault>
              <value>
                <struct>
                  <member>
                    <name>faultCode</name>
                    <value><int>4</int></value>
                  </member>
                  <member>
                    <name>faultString</name>
                    <value><string>Too many parameters.</string></value>
                  </member>
                </struct>
              </value>
            </fault>
          </methodResponse>
        ).swap.toOption.get.head
      )
    }

    it("should support ISO8601 datetime serialization") {
      val currentDate = new Date()

      assert(
        withoutMillis(currentDate) ===
          readXmlResponse[Date](writeXmlRequest[Date]("getSetDate", Some(currentDate)).asResponse).toOption.get
      )
    }

    it("should support <i4> xml tag inside a value") {
      val number = 14
      val responseWithI4 =
        <methodResponse>
          <params>
            <param>
              <value><i4>{number}</i4></value>
            </param>
          </params>
        </methodResponse>

      assert(
        number ===
          readXmlResponse[Int](responseWithI4).toOption.get
      )
    }

    it("should support empty xml tag, representing a string, inside a value") {
      val message = "Hello World!"

      val responseWithEmptyTag =
        <methodResponse>
          <params>
            <param>
              <value>{message}</value>
            </param>
          </params>
        </methodResponse>

      assert(
        message ===
          readXmlResponse[String](responseWithEmptyTag).toOption.get
      )
    }
  }
}
