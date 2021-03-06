package com.big.daddy.persistence

import com.big.daddy.{User, UserNotFound}
import zio.{Ref, Task, ZLayer}

final case class Test(users: Ref[Vector[User]]) extends Persistence.Service[User] {
  def get(id: Int): Task[User] =
    users.get.flatMap(users => Task.require(UserNotFound(id))(Task.succeed(users.find(_.id == id))))
  def create(user: User): Task[User] =
    users.update(_ :+ user).map(_ => user)
  def delete(id: Int): Task[Boolean] =
    users.modify(users => true -> users.filterNot(_.id == id))
}

object Test {
  val layer: ZLayer[Any, Nothing, UserPersistence] =
    ZLayer.fromEffect(Ref.make(Vector.empty[User]).map(Test(_)))
}
