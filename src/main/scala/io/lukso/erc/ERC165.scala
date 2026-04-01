package io.lukso.erc

import io.lukso.abi.AbiCodec
import io.lukso.rpc.{JsonRpcClient, RpcError}
import zio.*

trait ERC165:
  /** Check if a contract supports a given ERC165 interface. */
  def supportsInterface(contractAddress: String, interfaceId: String): IO[RpcError, Boolean]

  /** Check multiple interfaces in parallel. Returns Map[interfaceName -> supported]. */
  def checkInterfaces(contractAddress: String, interfaces: Map[String, String]): IO[RpcError, Map[String, Boolean]]

object ERC165:
  def supportsInterface(contractAddress: String, interfaceId: String): ZIO[ERC165, RpcError, Boolean] =
    ZIO.serviceWithZIO(_.supportsInterface(contractAddress, interfaceId))

  def checkInterfaces(contractAddress: String, interfaces: Map[String, String]): ZIO[ERC165, RpcError, Map[String, Boolean]] =
    ZIO.serviceWithZIO(_.checkInterfaces(contractAddress, interfaces))

  val layer: ZLayer[JsonRpcClient, Nothing, ERC165] =
    ZLayer.fromFunction { (rpc: JsonRpcClient) =>
      new ERC165:
        override def supportsInterface(contractAddress: String, interfaceId: String): IO[RpcError, Boolean] =
          val calldata = AbiCodec.encodeSupportsInterface(interfaceId)
          rpc.ethCall(contractAddress, calldata)
            .map(AbiCodec.decodeBool)
            .catchAll(_ => ZIO.succeed(false))

        override def checkInterfaces(contractAddress: String, interfaces: Map[String, String]): IO[RpcError, Map[String, Boolean]] =
          ZIO.foreachPar(interfaces.toList) { case (name, id) =>
            supportsInterface(contractAddress, id).map(name -> _)
          }.map(_.toMap)
    }
