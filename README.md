
# Xmlrpc for Scala [![Build Status](https://travis-ci.org/jvican/xmlrpc.svg?branch=master)](https://travis-ci.org/jvican/xmlrpc)
This is a Scala library to talk to servers via [XML-RPC](https://en.wikipedia.org/wiki/XML-RPC), originally created to connect to my university servers.This implementation is compliant with the [specification](http://xmlrpc.scripting.com/spec.html).

# What is XML-RPC?
As said in the specification:
> XML-RPC is a Remote Procedure Calling protocol that works over the Internet. An XML-RPC message is an HTTP-POST request. The body of the request is in XML. A procedure executes on the server and the value it returns is also formatted in XML.
This library uses [Spray](https://github.com/spray/spray) to connect to any HTTP server and with the help of Scalaz, it has good feedback in case of any failure.

# What does this solve?
It solves the problem of serializing and deserializing Scala types in a fancy way. Moreover, it does so simpler than other libraries, using the power of _type classes_ and _implicits_. This technique was proposed by _David McIver_ in [sbinary](https://github.com/harrah/sbinary) and it's very powerful, being used broadly in json and xml libraries.

# Usage
To be completed...

# Issues
At this moment, the only element that is not available to use directly is an Array of arbitrary types. In case this is needed, it's better to use a case class if possible.
