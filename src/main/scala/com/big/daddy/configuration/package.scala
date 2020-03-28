package com.big.daddy

import pureconfig.ConfigSource
import zio._

package configuration {

  final case class AppConfig(api: ApiConfig, dbConfig: DbConfig)
  final case class ApiConfig(endpoint: String, port: Int)
  final case class DbConfig(url: String, user: String, password: String)

  object Configuration {
    type Configuration = Has[ApiConfig] with Has[DbConfig]

    val apiConfig: ZIO[Has[ApiConfig], Throwable, ApiConfig] = ZIO.access(_.get)
    val dbConfig: ZIO[Has[DbConfig], Throwable, DbConfig]    = ZIO.access(_.get)

    import pureconfig.generic.auto._
    val live: Layer[Throwable, Configuration] = ZLayer.fromEffectMany(
        Task
        .effect(ConfigSource.default.loadOrThrow[AppConfig])
        .map(c => Has(c.api) ++ Has(c.dbConfig))
    )
  }
}
