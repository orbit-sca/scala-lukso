package io.lukso.rpc

import zio.json.*
import zio.json.ast.Json

/** JSON-RPC request envelope. */
final case class JsonRpcRequest(
  jsonrpc: String = "2.0",
  method:  String,
  params:  List[Json],
  id:      Long
)
object JsonRpcRequest:
  given JsonEncoder[JsonRpcRequest] = DeriveJsonEncoder.gen

/** JSON-RPC response envelope. */
final case class JsonRpcResponse(
  jsonrpc: String,
  id:      Option[Long],
  result:  Option[Json],
  error:   Option[JsonRpcError]
)
object JsonRpcResponse:
  given JsonDecoder[JsonRpcResponse] = DeriveJsonDecoder.gen

final case class JsonRpcError(code: Int, message: String)
object JsonRpcError:
  given JsonDecoder[JsonRpcError] = DeriveJsonDecoder.gen

/** Transaction from eth_getTransactionByHash. */
final case class RpcTransaction(
  hash:        String,
  from:        Option[String],
  to:          Option[String],
  value:       Option[String],
  input:       Option[String],
  nonce:       Option[String],
  blockNumber: Option[String],
  gas:         Option[String],
  gasPrice:    Option[String]
)
object RpcTransaction:
  given JsonDecoder[RpcTransaction] = DeriveJsonDecoder.gen
  given JsonEncoder[RpcTransaction] = DeriveJsonEncoder.gen

/** Transaction receipt from eth_getTransactionReceipt. */
final case class RpcTransactionReceipt(
  transactionHash: String,
  status:          Option[String],
  blockNumber:     Option[String],
  gasUsed:         Option[String],
  logs:            List[RpcLog]
)
object RpcTransactionReceipt:
  given JsonDecoder[RpcTransactionReceipt] = DeriveJsonDecoder.gen
  given JsonEncoder[RpcTransactionReceipt] = DeriveJsonEncoder.gen

/** Event log from receipt or eth_getLogs. */
final case class RpcLog(
  address:         String,
  topics:          List[String],
  data:            String,
  blockNumber:     Option[String],
  transactionHash: Option[String],
  logIndex:        Option[String]
)
object RpcLog:
  given JsonDecoder[RpcLog] = DeriveJsonDecoder.gen
  given JsonEncoder[RpcLog] = DeriveJsonEncoder.gen
