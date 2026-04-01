package io.lukso.lsp

import io.lukso.indexer.{EnvioClient, EnvioError}
import io.lukso.rpc.RpcError
import zio.*

trait LSP26:
  /** Get follower addresses for a UP. */
  def getFollowers(address: String, limit: Int = 20): IO[RpcError, List[String]]

  /** Get following addresses for a UP. */
  def getFollowing(address: String, limit: Int = 20): IO[RpcError, List[String]]

  /** Get follower count. */
  def getFollowerCount(address: String): IO[RpcError, Int]

  /** Get following count. */
  def getFollowingCount(address: String): IO[RpcError, Int]

object LSP26:
  def getFollowers(address: String, limit: Int = 20): ZIO[LSP26, RpcError, List[String]] =
    ZIO.serviceWithZIO(_.getFollowers(address, limit))

  def getFollowing(address: String, limit: Int = 20): ZIO[LSP26, RpcError, List[String]] =
    ZIO.serviceWithZIO(_.getFollowing(address, limit))

  def getFollowerCount(address: String): ZIO[LSP26, RpcError, Int] =
    ZIO.serviceWithZIO(_.getFollowerCount(address))

  def getFollowingCount(address: String): ZIO[LSP26, RpcError, Int] =
    ZIO.serviceWithZIO(_.getFollowingCount(address))

  val layer: ZLayer[EnvioClient, Nothing, LSP26] =
    ZLayer.fromFunction { (envio: EnvioClient) =>
      new LSP26:
        private def wrapEnvioError[A](effect: IO[EnvioError, A]): IO[RpcError, A] =
          effect.mapError(e => RpcError(s"Envio error: ${e.message}"))

        override def getFollowers(address: String, limit: Int): IO[RpcError, List[String]] =
          wrapEnvioError(envio.getFollowGraph(address, limit)).map(_.followers)

        override def getFollowing(address: String, limit: Int): IO[RpcError, List[String]] =
          wrapEnvioError(envio.getFollowGraph(address, limit)).map(_.following)

        override def getFollowerCount(address: String): IO[RpcError, Int] =
          wrapEnvioError(envio.getFollowGraph(address, 0)).map(_.followersCount)

        override def getFollowingCount(address: String): IO[RpcError, Int] =
          wrapEnvioError(envio.getFollowGraph(address, 0)).map(_.followingCount)
    }
