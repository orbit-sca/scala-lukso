package io.lukso.rpc

import zio.*
import zio.json.ast.Json

/** Generic Ethereum JSON-RPC client trait. */
trait JsonRpcClient:
  def call(method: String, params: List[Json]): IO[RpcError, Json]

  def ethCall(to: String, data: String, blockTag: String = "latest"): IO[RpcError, String]
  def ethGetCode(address: String, blockTag: String = "latest"): IO[RpcError, String]
  def ethGetBalance(address: String, blockTag: String = "latest"): IO[RpcError, String]
  def ethGetTransactionByHash(hash: String): IO[RpcError, Option[RpcTransaction]]
  def ethGetTransactionReceipt(hash: String): IO[RpcError, Option[RpcTransactionReceipt]]
  def ethGetLogs(fromBlock: Long, toBlock: Long, addresses: List[String], topics: List[Option[String]]): IO[RpcError, List[RpcLog]]
  def ethBlockNumber(): IO[RpcError, Long]
  def ethGetBlockByNumber(blockNumber: String, fullTx: Boolean): IO[RpcError, Json]
  def ethGasPrice(): IO[RpcError, BigInt]
  def ethChainId(): IO[RpcError, Int]

case class RpcError(message: String, code: Option[Int] = None) extends Exception(message)

object JsonRpcClient:
  def call(method: String, params: List[Json]): ZIO[JsonRpcClient, RpcError, Json] =
    ZIO.serviceWithZIO(_.call(method, params))

  def ethCall(to: String, data: String): ZIO[JsonRpcClient, RpcError, String] =
    ZIO.serviceWithZIO(_.ethCall(to, data))

  def ethGetCode(address: String): ZIO[JsonRpcClient, RpcError, String] =
    ZIO.serviceWithZIO(_.ethGetCode(address))

  def ethGetBalance(address: String): ZIO[JsonRpcClient, RpcError, String] =
    ZIO.serviceWithZIO(_.ethGetBalance(address))

  def ethBlockNumber(): ZIO[JsonRpcClient, RpcError, Long] =
    ZIO.serviceWithZIO(_.ethBlockNumber())

  def ethGasPrice(): ZIO[JsonRpcClient, RpcError, BigInt] =
    ZIO.serviceWithZIO(_.ethGasPrice())

  def ethChainId(): ZIO[JsonRpcClient, RpcError, Int] =
    ZIO.serviceWithZIO(_.ethChainId())
