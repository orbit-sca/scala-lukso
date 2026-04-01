package io.lukso.erc

import io.lukso.abi.AbiCodec
import io.lukso.rpc.{JsonRpcClient, RpcError}
import zio.*

trait ERC1271:
  /** Verify an ERC-1271 signature. Returns true if magic value 0x1626ba7e is returned. */
  def isValidSignature(contractAddress: String, hash: String, signature: String): IO[RpcError, Boolean]

object ERC1271:
  val MAGIC_VALUE = "0x1626ba7e"

  def isValidSignature(contractAddress: String, hash: String, signature: String): ZIO[ERC1271, RpcError, Boolean] =
    ZIO.serviceWithZIO(_.isValidSignature(contractAddress, hash, signature))

  val layer: ZLayer[JsonRpcClient, Nothing, ERC1271] =
    ZLayer.fromFunction { (rpc: JsonRpcClient) =>
      new ERC1271:
        override def isValidSignature(contractAddress: String, hash: String, signature: String): IO[RpcError, Boolean] =
          val calldata = AbiCodec.encodeIsValidSignature(hash, signature)
          rpc.ethCall(contractAddress, calldata)
            .map(result => result.stripPrefix("0x").take(8).toLowerCase == MAGIC_VALUE.stripPrefix("0x"))
            .catchAll(_ => ZIO.succeed(false))
    }
