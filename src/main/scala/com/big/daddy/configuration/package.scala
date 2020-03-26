package com.big.daddy

import pureconfig.ConfigSource
import zio.{Has, Layer, Task, ZIO, ZLayer}

package object configuration {

  type Configuration = Has[ApiConfig] with Has[DbConfig]

  final case class AppConfig(api: ApiConfig, dbConfig: DbConfig)
  final case class ApiConfig(endpoint: String, port: Int)
  final case class DbConfig(url: String, user: String, password: String)

  val apiConfig: ZIO[Has[ApiConfig], Throwable, ApiConfig] = ZIO.access(_.get)
  val dbConfig: ZIO[Has[DbConfig], Throwable, DbConfig]    = ZIO.access(_.get)

  object Configuration {
    val live: Layer[Throwable, Configuration] = ZLayer.fromEffectMany(
      Task
        .effect(ConfigSource.default.loadOrThrow[AppConfig])
        .map(c => Has(c.api) ++ Has(c.dbConfig))
    )
  }
}
