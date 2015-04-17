val MethodName = "getUserInfo"
case class Subject(code: Int, title: String)
case class UserInfo(name: String, age: Int, currentGrade: Double, subjects: Array[Subject])
/*Xmlrpc.invokeMethod[Int, UserInfo](MethodName, 41623) onComplete {
  case Success(deserialized) => deserialized
  case Failure(_) => s"An error while connecting to the server has occurred"
}*/

