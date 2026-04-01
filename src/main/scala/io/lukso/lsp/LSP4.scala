package io.lukso.lsp

import io.lukso.abi.AbiCodec
import io.lukso.erc.ERC725Y
import io.lukso.models.Asset
import io.lukso.rpc.{JsonRpcClient, RpcError}
import zio.*

trait LSP4:
  /** Read LSP4 metadata from a token/NFT contract. */
  def getAssetMetadata(contractAddress: String): IO[RpcError, Asset.Metadata]

  /** Get the LSP4TokenType (0=Token, 1=NFT, 2=Collection). */
  def getTokenType(contractAddress: String): IO[RpcError, Int]

object LSP4:
  def getAssetMetadata(contractAddress: String): ZIO[LSP4, RpcError, Asset.Metadata] =
    ZIO.serviceWithZIO(_.getAssetMetadata(contractAddress))

  def getTokenType(contractAddress: String): ZIO[LSP4, RpcError, Int] =
    ZIO.serviceWithZIO(_.getTokenType(contractAddress))

  val layer: ZLayer[ERC725Y & JsonRpcClient, Nothing, LSP4] =
    ZLayer.fromFunction { (erc725y: ERC725Y, rpc: JsonRpcClient) =>
      new LSP4:
        override def getAssetMetadata(contractAddress: String): IO[RpcError, Asset.Metadata] =
          for
            nameResult   <- rpc.ethCall(contractAddress, AbiCodec.encodeName()).catchAll(_ => ZIO.succeed("0x"))
            symbolResult <- rpc.ethCall(contractAddress, AbiCodec.encodeSymbol()).catchAll(_ => ZIO.succeed("0x"))
            typeResult   <- getTokenType(contractAddress).catchAll(_ => ZIO.succeed(-1))
            decResult    <- rpc.ethCall(contractAddress, AbiCodec.encodeDecimals())
                              .map(h => AbiCodec.decodeUint256(h).intValue)
                              .catchAll(_ => ZIO.succeed(18))
            supplyResult <- rpc.ethCall(contractAddress, AbiCodec.encodeTotalSupply())
                              .map(h => BigInt(AbiCodec.decodeUint256(h)))
                              .catchAll(_ => ZIO.succeed(BigInt(0)))
          yield Asset.Metadata(
            name        = AbiCodec.decodeString(nameResult),
            symbol      = AbiCodec.decodeString(symbolResult),
            tokenType   = typeResult,
            decimals    = decResult,
            totalSupply = supplyResult
          )

        override def getTokenType(contractAddress: String): IO[RpcError, Int] =
          erc725y.getData(contractAddress, DataKeys.LSP4TokenType).map { hex =>
            val clean = hex.stripPrefix("0x").replaceAll("^0+", "")
            if clean.isEmpty then 0 else Integer.parseInt(clean.take(8), 16)
          }
    }
