package com.big.daddy

final case class User(id: Int, name: String)

final case class UserNotFound(id: Int) extends Exception
