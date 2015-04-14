package xmlrpc

import org.scalatest.FunSpec
import xmlrpc.Deserializer.Fault

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

    it("should serialize arrays") {
      val primes = Vector(2, 3, 5, 7, 11, 13)

      assert(
        writeXmlRequest[Seq[Int]]("getSum", Some(primes)) ===
        <methodCall>
          <methodName>{"getSum"}</methodName>
          <params>
            <param>
              <array><data>
                <value><int>{primes.head}</int></value>
                <value><int>{primes(1)}</int></value>
                <value><int>{primes(2)}</int></value>
                <value><int>{primes(3)}</int></value>
                <value><int>{primes(4)}</int></value>
                <value><int>{primes.last}</int></value>
              </data></array>
            </param>
          </params>
        </methodCall>
      )
    }

    it("should serialize structs") {
      val bounds = Map("lowerBound" -> 18, "upperBound" -> 139)
      val methodName = "setBounds"

      assert(
        writeXmlRequest[Map[String, Int]](methodName, Some(bounds)) ===
        <methodCall>
          <methodName>{methodName}</methodName>
          <params>
            <param>
              <struct>
                <member>
                  <name>{"lowerBound"}</name>
                  <value><int>{bounds.get("lowerBound").get}</int></value>
                </member>
                <member>
                  <name>{"upperBound"}</name>
                  <value><int>{bounds.get("upperBound").get}</int></value>
                </member>
              </struct>
            </param>
          </params>
        </methodCall>
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
  }
}
