package com.big.daddy
package persistence

import cats.effect.Blocker
import com.big.daddy.configuration.{Configuration, DbConfig}
import doobie.h2.H2Transactor
import doobie.implicits._
import doobie.util.query.Query0
import doobie.util.transactor.Transactor
import doobie.util.update.Update0
import zio.blocking.Blocking
import zio.{Has, Managed, Reservation, Task, ZIO, ZLayer, blocking}
import zio.interop.catz._

import scala.concurrent.ExecutionContext

final class UserPersistenceService(tnx: Transactor[Task]) extends Persistence.Service[User] {
  import UserPersistenceService._

  def get(id: Int): Task[User] =
    SQL
      .get(id)
      .option
      .transact(tnx)
      .foldM(
          err => Task.fail(err)
        , maybeUser => Task.require(UserNotFound(id))(Task.succeed(maybeUser))
      )

  def create(user: User): Task[User] =
    SQL
      .create(user)
      .run
      .transact(tnx)
      .foldM(
          err => Task.fail(err)
        , _ => Task.succeed(user)
      )

  def delete(id: Int): Task[Boolean] =
    SQL
      .delete(id)
      .run
      .transact(tnx)
      .fold(_ => false, _ => true)
}

object UserPersistenceService {
  object SQL {
    def get(id: Int): Query0[User]  = sql"""SELECT * FROM USERS WHERE ID = $id """.query[User]
    def create(user: User): Update0 = sql"""INSERT INTO USERS (id, name) VALUES (${user.id}, ${user.name})""".update
    def delete(id: Int): Update0    = sql"""DELETE FROM USERS WHERE id = $id""".update
  }

  def mkTransactor(
        conf: DbConfig
      , connectEC: ExecutionContext
      , transactEC: ExecutionContext
  ): Managed[Throwable, UserPersistenceService] = {
    val xa = H2Transactor
      .newH2Transactor[Task](conf.url, conf.user, conf.password, connectEC, Blocker.liftExecutionContext(transactEC))

    val res = xa
      .allocated
      .map {
        case (transactor, cleanupM) =>
          Reservation(ZIO.succeed(transactor), _ => cleanupM.orDie)
      }
      .uninterruptible

    Managed(res)
      .map(new UserPersistenceService(_))
  }

  def live(connectEC: ExecutionContext): ZLayer[Has[DbConfig] with Blocking, Throwable, UserPersistenceService] =
    ZLayer.fromManaged {
      for {
        config     <- configuration.dbConfig.toManaged_
        blockingEC <- blocking.blocking { ZIO.descriptor.map(_.executor.asEC) }.toManaged_
        managed    <- mkTransactor(config, connectEC, blockingEC)
      } yield managed
    }
}
