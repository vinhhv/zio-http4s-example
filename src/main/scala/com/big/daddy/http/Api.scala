package com.big.daddy
package http

import io.circe.{Decoder, Encoder}
import io.circe.generic.auto._
import org.http4s.{EntityDecoder, EntityEncoder, HttpRoutes}
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import zio.RIO
import zio.interop.catz._

import persistence._

final case class Api[R <: UserPersistence](rootUri: String) {

  type UserTask[A] = RIO[R, A]

  implicit def circeJsonDecoder[A](implicit decoder: Decoder[A]): EntityDecoder[UserTask, A] =
    jsonOf[UserTask, A]
  implicit def circeJsonEncoder[A](implicit encoder: Encoder[A]): EntityEncoder[UserTask, A] =
    jsonEncoderOf[UserTask, A]

  val dsl: Http4sDsl[UserTask] = Http4sDsl[UserTask]
  import dsl._

  def routes: HttpRoutes[UserTask] =
    HttpRoutes.of[UserTask] {
      case GET -> Root / IntVar(id) => getUser(id).foldM(_ => NotFound(), Ok(_))
      case request @ POST -> Root =>
        request.decode[User] { user =>
          Created(createUser(user))
        }
      case DELETE -> Root / IntVar(id) =>
        (getUser(id) *> deleteUser(id)).foldM(_ => NotFound(), Ok(_))
    }
}
