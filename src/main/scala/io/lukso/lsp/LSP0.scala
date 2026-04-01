package io.lukso.lsp

import io.lukso.abi.AbiCodec
import io.lukso.erc.ERC165
import io.lukso.rpc.{JsonRpcClient, RpcError}
import zio.*

trait LSP0:
  /** Check if an address is a Universal Profile (supports LSP0 interface). */
  def isUniversalProfile(address: String): IO[RpcError, Boolean]

  /** Get the owner (Key Manager) of a Universal Profile. */
  def getOwner(address: String): IO[RpcError, String]

object LSP0:
  def isUniversalProfile(address: String): ZIO[LSP0, RpcError, Boolean] =
    ZIO.serviceWithZIO(_.isUniversalProfile(address))

  def getOwner(address: String): ZIO[LSP0, RpcError, String] =
    ZIO.serviceWithZIO(_.getOwner(address))

  val layer: ZLayer[ERC165 & JsonRpcClient, Nothing, LSP0] =
    ZLayer.fromFunction { (erc165: ERC165, rpc: JsonRpcClient) =>
      new LSP0:
        override def isUniversalProfile(address: String): IO[RpcError, Boolean] =
          erc165.supportsInterface(address, InterfaceIds.LSP0)

        override def getOwner(address: String): IO[RpcError, String] =
          val calldata = AbiCodec.encodeOwner()
          rpc.ethCall(address, calldata).map(AbiCodec.decodeAddress)
    }
