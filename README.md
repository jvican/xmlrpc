
# Xmlrpc for Scala [![Build Status](https://travis-ci.org/jvican/xmlrpc.svg?branch=master)](https://travis-ci.org/jvican/xmlrpc)
This is a Scala library to talk to servers via [XML-RPC](https://en.wikipedia.org/wiki/XML-RPC), originally created to connect to my university servers.This implementation is compliant with the [specification](http://xmlrpc.scripting.com/spec.html).

# What is XML-RPC?
As said in the specification:
> XML-RPC is a Remote Procedure Calling protocol that works over the Internet. An XML-RPC message is an HTTP-POST request. The body of the request is in XML. A procedure executes on the server and the value it returns is also formatted in XML.
  
This library uses [Spray](https://github.com/spray/spray) to connect to any HTTP server and with the help of Scalaz, it has good feedback in case of any failure.

# What does this solve?
It solves the problem of serializing and deserializing Scala types in a fancy way. Moreover, it does so simpler than other libraries, using the power of _type classes_ and _implicits_. This technique was proposed by _David McIver_ in [sbinary](https://github.com/harrah/sbinary) and it's very powerful, being used broadly in json and xml libraries.

# Usage
A tiny example using _case classes_. _Tuples_, _Option[T]_ and roughly any standard type can be used to read and write XML-RPC messages. This example only shows the serialization and deserialization but not the invokeMethod that can be used importing __xmlrpc.Xmlrpc__ (usage example in the future).
```scala
import xmlrpc.protocol.XmlrpcProtocol._

// As you see, no boilerplate code is needed
case class Subject(code: Int, title: String)
case class Student(name: String, age: Int, currentGrade: Double, favorite: Subject)

val history = Subject(1, "Contemporary History")
val charles = Student("Charles de Gaulle", 42, 7.2, history)

// The only restriction is to explicitly mark the type
writeXmlRequest[Student]("addStudent", Some(charles))

// This is a confirmation from the server
case class Confirmation(message: String)

// Make sure you always mark the return type
readXmlResponse[Confirmation](<methodResponse>
  <params>
    <param>
      <value><string>{"Ok"}</string></value>
    </param>
  </params>
</methodResponse>)
```

# Issues
At this moment, the only element that is not available to use directly is an Array of arbitrary types. In case this is needed, it's better to use a case class if possible.
