package io.lukso.rpc

import io.lukso.LuksoConfig
import zio.*
import zio.http.*
import zio.json.*
import zio.json.ast.Json

import java.util.concurrent.atomic.AtomicLong

/** Multi-endpoint fallback JSON-RPC provider.
  * Tries each endpoint sequentially with timeout, returns first success. */
final class RpcProvider(
  client:        Client,
  endpoints:     List[String],
  timeoutMs:     Long,
  customHeaders: Map[String, String] = Map.empty
) extends JsonRpcClient:

  private val idCounter = new AtomicLong(1)

  override def call(method: String, params: List[Json]): IO[RpcError, Json] =
    ZIO.foldLeft(endpoints)(Option.empty[Json]) { (acc, endpoint) =>
      acc match
        case Some(_) => ZIO.succeed(acc)
        case None =>
          callSingle(endpoint, method, params)
            .map(Some(_))
            .catchAll(_ => ZIO.succeed(None))
    }.flatMap {
      case Some(result) => ZIO.succeed(result)
      case None         => ZIO.fail(RpcError(s"All ${endpoints.size} RPC endpoints failed for $method"))
    }

  private def callSingle(endpoint: String, method: String, params: List[Json]): IO[RpcError, Json] =
    val id = idCounter.getAndIncrement()
    val request = JsonRpcRequest(method = method, params = params, id = id)
    val bodyStr = request.toJson

    (for
      url      <- ZIO.fromEither(URL.decode(endpoint)).mapError(e => RpcError(s"Bad URL $endpoint: $e"))
      base      = Request.post(url, Body.fromString(bodyStr))
                    .addHeader(Header.ContentType(MediaType.application.json))
      withHdrs  = customHeaders.foldLeft(base) { case (req, (k, v)) =>
                    req.addHeader(Header.Custom(k, v))
                  }
      response <- ZIO.scoped {
                    client.request(withHdrs)
                      .mapError(e => RpcError(s"HTTP error on $endpoint: ${e.getMessage}"))
                  }
      body     <- response.body.asString.mapError(e => RpcError(s"Body read error: ${e.getMessage}"))
      rpcResp  <- ZIO.fromEither(body.fromJson[JsonRpcResponse])
                    .mapError(e => RpcError(s"JSON parse error: $e"))
      result   <- rpcResp.error match
                    case Some(err) => ZIO.fail(RpcError(err.message, Some(err.code)))
                    case None =>
                      rpcResp.result match
                        case Some(r) => ZIO.succeed(r)
                        case None    => ZIO.fail(RpcError("null result"))
    yield result).timeoutFail(RpcError(s"Timeout after ${timeoutMs}ms on $endpoint"))(Duration.fromMillis(timeoutMs))

  // ── Convenience implementations ──

  override def ethCall(to: String, data: String, blockTag: String): IO[RpcError, String] =
    val params = List(
      Json.Obj("to" -> Json.Str(to), "data" -> Json.Str(data)),
      Json.Str(blockTag)
    )
    call("eth_call", params).flatMap {
      case Json.Str(hex) => ZIO.succeed(hex)
      case other         => ZIO.fail(RpcError(s"Expected hex string from eth_call, got: $other"))
    }

  override def ethGetCode(address: String, blockTag: String): IO[RpcError, String] =
    call("eth_getCode", List(Json.Str(address), Json.Str(blockTag))).flatMap {
      case Json.Str(hex) => ZIO.succeed(hex)
      case other         => ZIO.fail(RpcError(s"Expected hex string, got: $other"))
    }

  override def ethGetBalance(address: String, blockTag: String): IO[RpcError, String] =
    call("eth_getBalance", List(Json.Str(address), Json.Str(blockTag))).flatMap {
      case Json.Str(hex) => ZIO.succeed(hex)
      case other         => ZIO.fail(RpcError(s"Expected hex string, got: $other"))
    }

  override def ethGetTransactionByHash(hash: String): IO[RpcError, Option[RpcTransaction]] =
    call("eth_getTransactionByHash", List(Json.Str(hash))).flatMap {
      case Json.Null => ZIO.succeed(None)
      case json =>
        ZIO.fromEither(json.toJson.fromJson[RpcTransaction])
          .mapError(e => RpcError(s"Tx decode error: $e"))
          .map(Some(_))
    }

  override def ethGetTransactionReceipt(hash: String): IO[RpcError, Option[RpcTransactionReceipt]] =
    call("eth_getTransactionReceipt", List(Json.Str(hash))).flatMap {
      case Json.Null => ZIO.succeed(None)
      case json =>
        ZIO.fromEither(json.toJson.fromJson[RpcTransactionReceipt])
          .mapError(e => RpcError(s"Receipt decode error: $e"))
          .map(Some(_))
    }

  override def ethGetLogs(fromBlock: Long, toBlock: Long, addresses: List[String], topics: List[Option[String]]): IO[RpcError, List[RpcLog]] =
    val filter = Json.Obj(
      "fromBlock" -> Json.Str(s"0x${fromBlock.toHexString}"),
      "toBlock"   -> Json.Str(s"0x${toBlock.toHexString}"),
      "address"   -> (if addresses.size == 1 then Json.Str(addresses.head)
                      else Json.Arr(addresses.map(Json.Str(_))*)),
      "topics"    -> Json.Arr(topics.map {
        case Some(t) => Json.Str(t)
        case None    => Json.Null
      }*)
    )
    call("eth_getLogs", List(filter)).flatMap { json =>
      ZIO.fromEither(json.toJson.fromJson[List[RpcLog]])
        .mapError(e => RpcError(s"Logs decode error: $e"))
    }

  override def ethBlockNumber(): IO[RpcError, Long] =
    call("eth_blockNumber", List.empty).flatMap {
      case Json.Str(hex) =>
        ZIO.attempt(java.lang.Long.parseLong(hex.stripPrefix("0x"), 16))
          .mapError(e => RpcError(s"Block number parse error: ${e.getMessage}"))
      case other => ZIO.fail(RpcError(s"Expected hex string, got: $other"))
    }

  override def ethGetBlockByNumber(blockNumber: String, fullTx: Boolean): IO[RpcError, Json] =
    call("eth_getBlockByNumber", List(Json.Str(blockNumber), Json.Bool(fullTx)))

  override def ethGasPrice(): IO[RpcError, BigInt] =
    call("eth_gasPrice", List.empty).flatMap {
      case Json.Str(hex) =>
        ZIO.attempt(BigInt(hex.stripPrefix("0x"), 16))
          .mapError(e => RpcError(s"Gas price parse error: ${e.getMessage}"))
      case other => ZIO.fail(RpcError(s"Expected hex string, got: $other"))
    }

  override def ethChainId(): IO[RpcError, Int] =
    call("eth_chainId", List.empty).flatMap {
      case Json.Str(hex) =>
        ZIO.attempt(Integer.parseInt(hex.stripPrefix("0x"), 16))
          .mapError(e => RpcError(s"Chain ID parse error: ${e.getMessage}"))
      case other => ZIO.fail(RpcError(s"Expected hex string, got: $other"))
    }

object RpcProvider:
  val layer: ZLayer[Client & LuksoConfig, Nothing, JsonRpcClient] =
    ZLayer.fromFunction { (client: Client, config: LuksoConfig) =>
      new RpcProvider(client, config.rpcEndpoints, config.rpcTimeoutMs, config.rpcHeaders)
    }
