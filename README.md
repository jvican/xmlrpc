
# Xmlrpc for Scala [![Build Status](https://travis-ci.org/jvican/xmlrpc.svg?branch=master)](https://travis-ci.org/jvican/xmlrpc)
This is a Scala library to talk to servers and clients via [XML-RPC](https://en.wikipedia.org/wiki/XML-RPC), originally created to connect to my university servers. This implementation is compliant with the [specification](http://xmlrpc.scripting.com/spec.html).

# What is XML-RPC?
Chances are, that if you are reading this, you already know what XML-RPC is. But, for the sake of completeness, here it is the definition from the specification:
> XML-RPC is a Remote Procedure Calling protocol that works over the Internet. An XML-RPC message is an HTTP-POST request. The body of the request is in XML. A procedure executes on the server and the value it returns is also formatted in XML.

Despite that more powerful and modern rpc protocols are used nowadays, I have written this to support connection to older servers.

# Dependencies
This library uses [Akka HTTP](http://doc.akka.io/docs/akka-http/current/index.html) to connect to any HTTP server. It would be easy to change that if one wants to use another library like [Dispatch](https://github.com/dispatch/dispatch). You are free to fork this project and make the necesssary changes in _Xmlrpc.scala_, located in the main package. 
  
Thanks to Scalaz, it offers good feedback in case of any failure with the help of __Validation[T]__. Validation is _applicative_, this means that all the errors in the process of deserialization will be accumulated.

Using [Shapeless](https://github.com/milessabin/shapeless), we solve the problem of writing boilerplate code for any arity of case classes and tuples. If you are more interested in a library for serialization using Shapeless, you can check [PicoPickle](https://github.com/netvl/picopickle), an extensible, more powerful library entirely written in Shapeless.

# Import to your project
This project is compatible with Scala 2.11 and 2.12.
  
In order to add it to your project, write the following in your build.sbt:
```scala
libraryDependencies ++= Seq("com.github.jvican" %% "xmlrpc" % "1.2")
```

# What does this solve?
It solves the problem of serializing and deserializing types in a fancy way. Moreover, it does so simpler than other libraries, using the power of _type classes_ and _implicits_. This technique was proposed by _David McIver_ in [sbinary](https://github.com/harrah/sbinary) and it's very powerful, being used broadly in json and xml libraries.

# Usage
## Serializing and Deserializing
A tiny example using _case classes_. _Tuples_, _Option[T]_ and roughly any standard type can be used to read and write XML-RPC messages (if you want for some type in particular, please let me know). This example only shows the serialization and deserialization but not the use of __invokeMethod__. If you are implementing a server, you may use only this feature to reply the client.
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

## Connecting to the server
First, import the library:
```scala
import xmlrpc.protocol.XmlrpcProtocol._
import xmlrpc.Xmlrpc._
```
  
If you don't have an Actor System in scope or you don't have an environment created for Akka HTTP, you must set it up:
```scala
implicit val system = ActorSystem()
implicit val ma = ActorMaterializer()
implicit val timeout = Timeout(5 seconds)
import system.dispatcher
```
  
Now, we set up the XML-RPC server and invoke any method:
```scala
implicit val testServer = XmlrpcServer("http://betty.userland.com/RPC2")
val response: XmlrpcResponse[Int] = invokeMethod[String, Int]("methodName", "Hello World!")
```  
You don't have to explicit the type of the response. If you want to have a __Future[Int]__, you can access the attribute _underlying_ of the response. __XmlrpcResponse__ is a wrapper useful when we want to invoke several methods, because it allow us to use for-comprehensions and chain them.

# Issues
At this moment, it's not possible to serialize and deserialize __Seq[Any]__. For example, you cannot serialize `List("a", 1)`. In case you need this, it's better to use case classes if the appearances of these types are cyclic, e.g. `List("a", 1, "a", 1)`. When I have more time, I would include this functionality in the library.
