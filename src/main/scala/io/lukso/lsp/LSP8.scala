package io.lukso.lsp

import io.lukso.abi.AbiCodec
import io.lukso.rpc.{JsonRpcClient, RpcError}
import zio.*

import scala.jdk.CollectionConverters.*

trait LSP8:
  def tokenIdsOf(nftAddress: String, ownerAddress: String): IO[RpcError, List[String]]
  def totalSupply(nftAddress: String): IO[RpcError, BigInt]
  def tokenOwnerOf(nftAddress: String, tokenId: String): IO[RpcError, String]

object LSP8:
  def tokenIdsOf(nftAddress: String, ownerAddress: String): ZIO[LSP8, RpcError, List[String]] =
    ZIO.serviceWithZIO(_.tokenIdsOf(nftAddress, ownerAddress))

  def totalSupply(nftAddress: String): ZIO[LSP8, RpcError, BigInt] =
    ZIO.serviceWithZIO(_.totalSupply(nftAddress))

  def tokenOwnerOf(nftAddress: String, tokenId: String): ZIO[LSP8, RpcError, String] =
    ZIO.serviceWithZIO(_.tokenOwnerOf(nftAddress, tokenId))

  val layer: ZLayer[JsonRpcClient, Nothing, LSP8] =
    ZLayer.fromFunction { (rpc: JsonRpcClient) =>
      new LSP8:
        override def tokenIdsOf(nftAddress: String, ownerAddress: String): IO[RpcError, List[String]] =
          rpc.ethCall(nftAddress, AbiCodec.encodeTokenIdsOf(ownerAddress)).map { hex =>
            val results = AbiCodec.decodeReturnValues(hex,
              List(new org.web3j.abi.TypeReference[org.web3j.abi.datatypes.DynamicArray[org.web3j.abi.datatypes.generated.Bytes32]]() {}))
            results.flatMap {
              case arr: org.web3j.abi.datatypes.DynamicArray[?] =>
                arr.getValue.asScala.map { item =>
                  val bytes = item.asInstanceOf[org.web3j.abi.datatypes.generated.Bytes32].getValue
                  "0x" + bytes.map("%02x".format(_)).mkString
                }.toList
              case _ => List.empty
            }
          }

        override def totalSupply(nftAddress: String): IO[RpcError, BigInt] =
          rpc.ethCall(nftAddress, AbiCodec.encodeTotalSupply())
            .map(h => BigInt(AbiCodec.decodeUint256(h)))

        override def tokenOwnerOf(nftAddress: String, tokenId: String): IO[RpcError, String] =
          rpc.ethCall(nftAddress, AbiCodec.encodeTokenOwnerOf(tokenId))
            .map(AbiCodec.decodeAddress)
    }
