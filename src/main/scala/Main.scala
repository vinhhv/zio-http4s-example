package com.big.daddy

import cats.effect.ExitCode
import com.big.daddy.configuration.Configuration
import com.big.daddy.configuration.Configuration.Configuration
import com.big.daddy.http.Api
import com.big.daddy.persistence.{UserPersistence, UserPersistenceService}
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.CORS
import zio._
import zio.blocking.Blocking
import zio.clock.Clock
import zio.console.putStrLn
import zio.interop.catz._

object Main extends App {
  type AppEnvironment = Configuration with Clock with UserPersistence
  type AppTask[A]     = RIO[AppEnvironment, A]

  val userPersistence =
    (Configuration.live ++ Blocking.live) >>> UserPersistenceService
      .live(platform.executor.asEC)

  override def run(args: List[String]): ZIO[ZEnv, Nothing, Int] = {
    val program: ZIO[AppEnvironment, Throwable, Unit] = for {
      api <- configuration.Configuration.apiConfig
      httpApp = Router[AppTask]("/users" -> Api(s"${api.endpoint}/users").routes).orNotFound
      server <- ZIO.runtime[AppEnvironment].flatMap { implicit rts =>
        BlazeServerBuilder[AppTask]
          .bindHttp(api.port, api.endpoint)
          .withHttpApp(CORS(httpApp))
          .serve
          .compile[AppTask, AppTask, ExitCode]
          .drain
      }
    } yield server

    program
      .provideSomeLayer[ZEnv](Configuration.live ++ userPersistence)
      .foldM(
          err => putStrLn(s"Execution failed with: $err") *> IO.succeed(1)
        , _ => IO.succeed(0)
      )
  }
}
