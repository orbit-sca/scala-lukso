package io.lukso.lsp

import io.lukso.abi.AbiCodec
import io.lukso.rpc.{JsonRpcClient, RpcError}
import zio.*

trait LSP7:
  def balanceOf(tokenAddress: String, holderAddress: String): IO[RpcError, BigInt]
  def totalSupply(tokenAddress: String): IO[RpcError, BigInt]
  def decimals(tokenAddress: String): IO[RpcError, Int]

object LSP7:
  def balanceOf(tokenAddress: String, holderAddress: String): ZIO[LSP7, RpcError, BigInt] =
    ZIO.serviceWithZIO(_.balanceOf(tokenAddress, holderAddress))

  def totalSupply(tokenAddress: String): ZIO[LSP7, RpcError, BigInt] =
    ZIO.serviceWithZIO(_.totalSupply(tokenAddress))

  def decimals(tokenAddress: String): ZIO[LSP7, RpcError, Int] =
    ZIO.serviceWithZIO(_.decimals(tokenAddress))

  val layer: ZLayer[JsonRpcClient, Nothing, LSP7] =
    ZLayer.fromFunction { (rpc: JsonRpcClient) =>
      new LSP7:
        override def balanceOf(tokenAddress: String, holderAddress: String): IO[RpcError, BigInt] =
          rpc.ethCall(tokenAddress, AbiCodec.encodeBalanceOf(holderAddress))
            .map(h => BigInt(AbiCodec.decodeUint256(h)))

        override def totalSupply(tokenAddress: String): IO[RpcError, BigInt] =
          rpc.ethCall(tokenAddress, AbiCodec.encodeTotalSupply())
            .map(h => BigInt(AbiCodec.decodeUint256(h)))

        override def decimals(tokenAddress: String): IO[RpcError, Int] =
          rpc.ethCall(tokenAddress, AbiCodec.encodeDecimals())
            .map(h => AbiCodec.decodeUint256(h).intValue)
    }
