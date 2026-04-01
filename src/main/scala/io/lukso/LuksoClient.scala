package io.lukso

import io.lukso.abi.{AbiCodec, FunctionSelectors, EventSignatures}
import io.lukso.erc.*
import io.lukso.indexer.*
import io.lukso.lsp.*
import io.lukso.models.*
import io.lukso.rpc.*
import zio.*
import zio.http.Client

/** High-level facade for all LUKSO operations.
  *
  * Usage:
  * {{{
  * val program = for
  *   profile <- LuksoClient.getProfile("0x...")
  *   _       <- Console.printLine(s"Name: ${profile.name}")
  * yield ()
  *
  * program.provide(LuksoClient.live, Client.default, ZLayer.succeed(LuksoConfig.default))
  * }}}
  */
trait LuksoClient:
  // ── Profile Operations ──
  def getProfile(address: String): IO[RpcError, Profile]
  def searchProfiles(query: String): IO[RpcError, List[(String, Option[String])]]

  // ── Asset Operations ──
  def getReceivedAssets(address: String): IO[RpcError, List[String]]
  def getIssuedAssets(address: String): IO[RpcError, List[String]]
  def getAssetMetadata(contractAddress: String): IO[RpcError, Asset.Metadata]
  def getTokenBalance(tokenAddress: String, holder: String): IO[RpcError, BigInt]

  // ── Permission Operations ──
  def getPermissions(upAddress: String, controller: String): IO[RpcError, Permission.Decoded]
  def hasPermission(upAddress: String, controller: String, permissionName: String): IO[RpcError, Boolean]

  // ── Contract Detection ──
  def isUniversalProfile(address: String): IO[RpcError, Boolean]
  def detectContractType(address: String): IO[RpcError, ContractInfo]

  // ── Social Graph ──
  def getFollowerCount(address: String): IO[RpcError, Int]
  def getFollowingCount(address: String): IO[RpcError, Int]
  def getFollowers(address: String, limit: Int = 20): IO[RpcError, List[String]]
  def getFollowing(address: String, limit: Int = 20): IO[RpcError, List[String]]

  // ── Transaction Operations ──
  def decodeTransaction(txHash: String): IO[RpcError, Transaction.Decoded]

  // ── Network ──
  def getBlockNumber(): IO[RpcError, Long]
  def getLyxBalance(address: String): IO[RpcError, BigInt]

object LuksoClient:

  // ── Service accessors ──
  def getProfile(address: String): ZIO[LuksoClient, RpcError, Profile] =
    ZIO.serviceWithZIO(_.getProfile(address))

  def searchProfiles(query: String): ZIO[LuksoClient, RpcError, List[(String, Option[String])]] =
    ZIO.serviceWithZIO(_.searchProfiles(query))

  def getReceivedAssets(address: String): ZIO[LuksoClient, RpcError, List[String]] =
    ZIO.serviceWithZIO(_.getReceivedAssets(address))

  def getIssuedAssets(address: String): ZIO[LuksoClient, RpcError, List[String]] =
    ZIO.serviceWithZIO(_.getIssuedAssets(address))

  def getAssetMetadata(contractAddress: String): ZIO[LuksoClient, RpcError, Asset.Metadata] =
    ZIO.serviceWithZIO(_.getAssetMetadata(contractAddress))

  def getTokenBalance(tokenAddress: String, holder: String): ZIO[LuksoClient, RpcError, BigInt] =
    ZIO.serviceWithZIO(_.getTokenBalance(tokenAddress, holder))

  def getPermissions(upAddress: String, controller: String): ZIO[LuksoClient, RpcError, Permission.Decoded] =
    ZIO.serviceWithZIO(_.getPermissions(upAddress, controller))

  def hasPermission(upAddress: String, controller: String, permissionName: String): ZIO[LuksoClient, RpcError, Boolean] =
    ZIO.serviceWithZIO(_.hasPermission(upAddress, controller, permissionName))

  def isUniversalProfile(address: String): ZIO[LuksoClient, RpcError, Boolean] =
    ZIO.serviceWithZIO(_.isUniversalProfile(address))

  def detectContractType(address: String): ZIO[LuksoClient, RpcError, ContractInfo] =
    ZIO.serviceWithZIO(_.detectContractType(address))

  def getFollowerCount(address: String): ZIO[LuksoClient, RpcError, Int] =
    ZIO.serviceWithZIO(_.getFollowerCount(address))

  def getFollowingCount(address: String): ZIO[LuksoClient, RpcError, Int] =
    ZIO.serviceWithZIO(_.getFollowingCount(address))

  def decodeTransaction(txHash: String): ZIO[LuksoClient, RpcError, Transaction.Decoded] =
    ZIO.serviceWithZIO(_.decodeTransaction(txHash))

  def getBlockNumber(): ZIO[LuksoClient, RpcError, Long] =
    ZIO.serviceWithZIO(_.getBlockNumber())

  def getLyxBalance(address: String): ZIO[LuksoClient, RpcError, BigInt] =
    ZIO.serviceWithZIO(_.getLyxBalance(address))

  /** Fully wired LuksoClient from config + HTTP client. */
  val live: ZLayer[Client & LuksoConfig, Nothing, LuksoClient] =
    // Wire layers bottom-up using ZLayer composition
    val rpcLayer    = RpcProvider.layer
    val erc165Layer = rpcLayer >>> ERC165.layer
    val erc725yLayer = rpcLayer >>> ERC725Y.layer
    val erc1271Layer = rpcLayer >>> ERC1271.layer
    val lsp0Layer   = (erc165Layer ++ rpcLayer) >>> LSP0.layer
    val lsp3Layer   = (erc725yLayer ++ ZLayer.service[Client] ++ ZLayer.service[LuksoConfig]) >>> LSP3.layer
    val lsp4Layer   = (erc725yLayer ++ rpcLayer) >>> LSP4.layer
    val lsp5Layer   = erc725yLayer >>> LSP5.layer
    val lsp6Layer   = (erc725yLayer ++ rpcLayer) >>> LSP6.layer
    val lsp7Layer   = rpcLayer >>> LSP7.layer
    val lsp8Layer   = rpcLayer >>> LSP8.layer
    val lsp12Layer  = erc725yLayer >>> LSP12.layer
    val envioLayer  = EnvioClient.layer
    val lsp26Layer  = envioLayer >>> LSP26.layer

    val allLayers = rpcLayer ++ erc165Layer ++ erc725yLayer ++ lsp0Layer ++ lsp3Layer ++
      lsp4Layer ++ lsp5Layer ++ lsp6Layer ++ lsp7Layer ++ lsp8Layer ++ lsp12Layer ++ lsp26Layer ++ envioLayer

    allLayers >>> ZLayer.fromFunction { (
      rpc: JsonRpcClient, erc165: ERC165, erc725y: ERC725Y,
      lsp0: LSP0, lsp3: LSP3, lsp4: LSP4, lsp5: LSP5, lsp6: LSP6,
      lsp7: LSP7, lsp8: LSP8, lsp12: LSP12, lsp26: LSP26, envio: EnvioClient
    ) =>
      new LuksoClientLive(rpc, erc165, erc725y, lsp0, lsp3, lsp4, lsp5, lsp6, lsp7, lsp8, lsp12, lsp26, envio): LuksoClient
    }

private final class LuksoClientLive(
  rpc:      JsonRpcClient,
  erc165:   ERC165,
  erc725y:  ERC725Y,
  lsp0:     LSP0,
  lsp3:     LSP3,
  lsp4:     LSP4,
  lsp5:     LSP5,
  lsp6:     LSP6,
  lsp7:     LSP7,
  lsp8:     LSP8,
  lsp12:    LSP12,
  lsp26:    LSP26,
  envio:    EnvioClient
) extends LuksoClient:

  override def getProfile(address: String): IO[RpcError, Profile] =
    lsp3.getProfile(address)

  override def searchProfiles(query: String): IO[RpcError, List[(String, Option[String])]] =
    envio.searchProfiles(query)
      .map(_.map(p => (p.address, p.name)))
      .mapError(e => RpcError(s"Search error: ${e.message}"))

  override def getReceivedAssets(address: String): IO[RpcError, List[String]] =
    lsp5.getReceivedAssets(address)

  override def getIssuedAssets(address: String): IO[RpcError, List[String]] =
    lsp12.getIssuedAssets(address)

  override def getAssetMetadata(contractAddress: String): IO[RpcError, Asset.Metadata] =
    lsp4.getAssetMetadata(contractAddress)

  override def getTokenBalance(tokenAddress: String, holder: String): IO[RpcError, BigInt] =
    lsp7.balanceOf(tokenAddress, holder)

  override def getPermissions(upAddress: String, controller: String): IO[RpcError, Permission.Decoded] =
    lsp6.getPermissions(upAddress, controller)

  override def hasPermission(upAddress: String, controller: String, permissionName: String): IO[RpcError, Boolean] =
    lsp6.hasPermission(upAddress, controller, permissionName)

  override def isUniversalProfile(address: String): IO[RpcError, Boolean] =
    lsp0.isUniversalProfile(address)

  override def detectContractType(address: String): IO[RpcError, ContractInfo] =
    for
      code       <- rpc.ethGetCode(address)
      isContract  = code != "0x" && code.length > 2
      interfaces <- if isContract then erc165.checkInterfaces(address, InterfaceIds.allStandard)
                    else ZIO.succeed(Map.empty[String, Boolean])
      detected    = interfaces.filter(_._2).keys.toList
      tokenInfo  <- if interfaces.getOrElse("LSP7", false) || interfaces.getOrElse("LSP8", false) then
                      lsp4.getAssetMetadata(address).map(Some(_)).catchAll(_ => ZIO.succeed(None))
                    else ZIO.succeed(None)
      cType       = if !isContract then "EOA"
                    else if interfaces.getOrElse("LSP0", false) then "UniversalProfile"
                    else if interfaces.getOrElse("LSP6", false) then "KeyManager"
                    else if interfaces.getOrElse("LSP7", false) then "LSP7Token"
                    else if interfaces.getOrElse("LSP8", false) then "LSP8Collection"
                    else if interfaces.getOrElse("LSP9", false) then "Vault"
                    else "UnknownContract"
    yield ContractInfo(
      address           = address.toLowerCase,
      isContract        = isContract,
      contractType      = cType,
      detectedStandards = detected,
      interfaceIds      = interfaces,
      tokenInfo         = tokenInfo
    )

  override def getFollowerCount(address: String): IO[RpcError, Int] =
    lsp26.getFollowerCount(address)

  override def getFollowingCount(address: String): IO[RpcError, Int] =
    lsp26.getFollowingCount(address)

  override def getFollowers(address: String, limit: Int): IO[RpcError, List[String]] =
    lsp26.getFollowers(address, limit)

  override def getFollowing(address: String, limit: Int): IO[RpcError, List[String]] =
    lsp26.getFollowing(address, limit)

  override def decodeTransaction(txHash: String): IO[RpcError, Transaction.Decoded] =
    for
      txOpt      <- rpc.ethGetTransactionByHash(txHash)
      tx         <- ZIO.fromOption(txOpt).mapError(_ => RpcError(s"Transaction not found: $txHash"))
      receiptOpt <- rpc.ethGetTransactionReceipt(txHash)
      receipt     = receiptOpt
      selector    = tx.input.getOrElse("0x").take(10).toLowerCase
      method      = FunctionSelectors.lookup(selector)
      events      = receipt.toList.flatMap(_.logs.map { log =>
                      val eventName = log.topics.headOption.flatMap(EventSignatures.lookup).getOrElse("Unknown")
                      Transaction.DecodedEvent(eventName, log.data)
                    })
    yield Transaction.Decoded(
      hash        = tx.hash,
      from        = tx.from.getOrElse(""),
      to          = tx.to,
      value       = tx.value.getOrElse("0x0"),
      status      = receipt.flatMap(_.status).getOrElse("unknown"),
      selector    = selector,
      method      = method.map(_.signature).getOrElse("unknown"),
      lspContext  = method.map(_.lspContext).getOrElse("unknown"),
      events      = events,
      gasUsed     = receipt.flatMap(_.gasUsed).getOrElse("0x0"),
      blockNumber = tx.blockNumber.flatMap(h => scala.util.Try(java.lang.Long.parseLong(h.stripPrefix("0x"), 16)).toOption),
      nonce       = tx.nonce.flatMap(h => scala.util.Try(java.lang.Long.parseLong(h.stripPrefix("0x"), 16)).toOption).getOrElse(0L)
    )

  override def getBlockNumber(): IO[RpcError, Long] =
    rpc.ethBlockNumber()

  override def getLyxBalance(address: String): IO[RpcError, BigInt] =
    rpc.ethGetBalance(address).map { hex =>
      val clean = hex.stripPrefix("0x")
      if clean.isEmpty then BigInt(0) else BigInt(clean, 16)
    }
