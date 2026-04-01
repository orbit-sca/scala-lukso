package io.lukso.indexer

import io.lukso.LuksoConfig
import zio.*
import zio.http.*
import zio.json.*
import zio.json.ast.Json

case class BlockscoutError(message: String) extends Exception(message)

trait BlockscoutClient:
  /** Get address info (balance, tx_count, is_contract). */
  def getAddress(address: String): IO[BlockscoutError, BlockscoutClient.AddressInfo]

  /** Get recent transactions for an address. */
  def getTransactions(address: String, limit: Int = 10): IO[BlockscoutError, List[BlockscoutClient.Transaction]]

  /** Get token balances. */
  def getTokenBalances(address: String): IO[BlockscoutError, List[BlockscoutClient.TokenBalance]]

  /** Get token transfers. */
  def getTokenTransfers(address: String, limit: Int = 50): IO[BlockscoutError, List[BlockscoutClient.TokenTransfer]]

object BlockscoutClient:
  final case class AddressInfo(
    coin_balance: Option[String],
    is_contract:  Option[Boolean],
    name:         Option[String],
    transactions_count: Option[Long]
  )
  object AddressInfo:
    given JsonDecoder[AddressInfo] = DeriveJsonDecoder.gen
    given JsonEncoder[AddressInfo] = DeriveJsonEncoder.gen

  final case class Transaction(
    hash:         String,
    status:       Option[String],
    method:       Option[String],
    block:        Option[Long],
    value:        Option[String],
    from:         Option[BsAddressHash],
    to:           Option[BsAddressHash],
    timestamp:    Option[String]
  )
  object Transaction:
    given JsonDecoder[Transaction] = DeriveJsonDecoder.gen
    given JsonEncoder[Transaction] = DeriveJsonEncoder.gen

  final case class BsAddressHash(hash: Option[String])
  object BsAddressHash:
    given JsonDecoder[BsAddressHash] = DeriveJsonDecoder.gen
    given JsonEncoder[BsAddressHash] = DeriveJsonEncoder.gen

  final case class TokenBalance(
    token: Option[BsToken],
    value: Option[String]
  )
  object TokenBalance:
    given JsonDecoder[TokenBalance] = DeriveJsonDecoder.gen
    given JsonEncoder[TokenBalance] = DeriveJsonEncoder.gen

  final case class BsToken(
    address_hash: Option[String],
    name:         Option[String],
    symbol:       Option[String],
    `type`:       Option[String],
    decimals:     Option[String]
  )
  object BsToken:
    given JsonDecoder[BsToken] = DeriveJsonDecoder.gen
    given JsonEncoder[BsToken] = DeriveJsonEncoder.gen

  final case class TokenTransfer(
    tx_hash:    Option[String],
    from:       Option[BsAddressHash],
    to:         Option[BsAddressHash],
    token:      Option[BsToken],
    total:      Option[BsTotal],
    timestamp:  Option[String]
  )
  object TokenTransfer:
    given JsonDecoder[TokenTransfer] = DeriveJsonDecoder.gen
    given JsonEncoder[TokenTransfer] = DeriveJsonEncoder.gen

  final case class BsTotal(value: Option[String], decimals: Option[String])
  object BsTotal:
    given JsonDecoder[BsTotal] = DeriveJsonDecoder.gen
    given JsonEncoder[BsTotal] = DeriveJsonEncoder.gen

  // Wrapper for paginated Blockscout responses
  private final case class BsPaginated[A](items: List[A])
  private object BsPaginated:
    given [A: JsonDecoder]: JsonDecoder[BsPaginated[A]] = DeriveJsonDecoder.gen

  // Service accessors
  def getAddress(address: String): ZIO[BlockscoutClient, BlockscoutError, AddressInfo] =
    ZIO.serviceWithZIO(_.getAddress(address))

  def getTransactions(address: String, limit: Int = 10): ZIO[BlockscoutClient, BlockscoutError, List[Transaction]] =
    ZIO.serviceWithZIO(_.getTransactions(address, limit))

  def getTokenBalances(address: String): ZIO[BlockscoutClient, BlockscoutError, List[TokenBalance]] =
    ZIO.serviceWithZIO(_.getTokenBalances(address))

  val layer: ZLayer[Client & LuksoConfig, Nothing, BlockscoutClient] =
    ZLayer.fromFunction { (httpClient: Client, config: LuksoConfig) =>
      new BlockscoutClient:
        private def get(path: String): IO[BlockscoutError, String] =
          val fullUrl = s"${config.blockscoutBase}$path"
          (for
            url  <- ZIO.fromEither(URL.decode(fullUrl)).mapError(e => BlockscoutError(s"Bad URL: $e"))
            resp <- ZIO.scoped {
                      httpClient.request(
                        Request.get(url)
                      ).mapError(e => BlockscoutError(s"HTTP error: ${e.getMessage}"))
                    }
            body <- resp.body.asString.mapError(e => BlockscoutError(s"Body read error: ${e.getMessage}"))
          yield body).timeoutFail(BlockscoutError("Blockscout timeout"))(Duration.fromMillis(10000))

        override def getAddress(address: String): IO[BlockscoutError, AddressInfo] =
          get(s"/addresses/$address").flatMap { body =>
            ZIO.fromEither(body.fromJson[AddressInfo])
              .mapError(e => BlockscoutError(s"JSON parse error: $e"))
          }

        override def getTransactions(address: String, limit: Int): IO[BlockscoutError, List[Transaction]] =
          get(s"/addresses/$address/transactions?limit=$limit").flatMap { body =>
            ZIO.fromEither(body.fromJson[BsPaginated[Transaction]])
              .map(_.items)
              .catchAll(_ =>
                // Blockscout sometimes returns bare array
                ZIO.fromEither(body.fromJson[List[Transaction]])
                  .mapError(e => BlockscoutError(s"JSON parse error: $e"))
              )
          }

        override def getTokenBalances(address: String): IO[BlockscoutError, List[TokenBalance]] =
          get(s"/addresses/$address/token-balances").flatMap { body =>
            ZIO.fromEither(body.fromJson[List[TokenBalance]])
              .mapError(e => BlockscoutError(s"JSON parse error: $e"))
          }

        override def getTokenTransfers(address: String, limit: Int): IO[BlockscoutError, List[TokenTransfer]] =
          get(s"/addresses/$address/token-transfers?limit=$limit").flatMap { body =>
            ZIO.fromEither(body.fromJson[BsPaginated[TokenTransfer]])
              .map(_.items)
              .catchAll(_ =>
                ZIO.fromEither(body.fromJson[List[TokenTransfer]])
                  .mapError(e => BlockscoutError(s"JSON parse error: $e"))
              )
          }
    }
