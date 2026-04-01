package io.lukso.lsp

import io.lukso.erc.ERC725Y
import io.lukso.models.Profile
import io.lukso.rpc.RpcError
import io.lukso.LuksoConfig
import zio.*
import zio.http.*
import zio.json.*

trait LSP3:
  /** Fetch full LSP3 profile metadata for a Universal Profile address.
    * Reads ERC725Y key, decodes JSONURL, fetches from IPFS, parses JSON. */
  def getProfile(address: String): IO[RpcError, Profile]

object LSP3:
  def getProfile(address: String): ZIO[LSP3, RpcError, Profile] =
    ZIO.serviceWithZIO(_.getProfile(address))

  val layer: ZLayer[ERC725Y & Client & LuksoConfig, Nothing, LSP3] =
    ZLayer.fromFunction { (erc725y: ERC725Y, client: Client, config: LuksoConfig) =>
      new LSP3:
        override def getProfile(address: String): IO[RpcError, Profile] =
          for
            rawData <- erc725y.getData(address, DataKeys.LSP3Profile)
            _       <- ZIO.when(rawData == "0x" || rawData.length < 10)(
                         ZIO.fail(RpcError("No LSP3Profile data found"))
                       )
            jsonUrl <- ZIO.fromEither(decodeJsonUrl(rawData))
                        .mapError(e => RpcError(s"JSONURL decode error: $e"))
            jsonStr <- fetchFromIpfs(jsonUrl, config.ipfsGateways, config.ipfsTimeoutMs, client)
            profile <- ZIO.fromEither(parseProfileJson(address, jsonStr))
                        .mapError(e => RpcError(s"Profile JSON parse error: $e"))
          yield profile

        /** Decode JSONURL encoding from ERC725Y value.
          * Format: hashFunction(4 hex = 2 bytes) + hash(64 hex = 32 bytes) + url(variable).
          * The 0x6f357c6a prefix means keccak256 + UTF-8 URL. */
        private def decodeJsonUrl(rawHex: String): Either[String, String] =
          val clean = rawHex.stripPrefix("0x")
          if clean.length < 68 then Left(s"Data too short for JSONURL (${clean.length} hex chars)")
          else
            val urlHex = clean.drop(68)
            if urlHex.isEmpty then Left("Empty URL in JSONURL")
            else
              val urlBytes = urlHex.grouped(2).map(Integer.parseInt(_, 16).toByte).toArray
              Right(new String(urlBytes, "UTF-8"))

        /** Try all IPFS gateways in parallel, return first success. */
        private def fetchFromIpfs(url: String, gateways: List[String], timeoutMs: Long, httpClient: Client): IO[RpcError, String] =
          val attempts = gateways.map { gw =>
            val fullUrl = if url.startsWith("ipfs://") then url.replace("ipfs://", gw)
                          else if url.startsWith("https://") || url.startsWith("http://") then url
                          else gw + url
            ZIO.scoped {
              httpClient.request(
                Request.get(URL.decode(fullUrl).getOrElse(URL.empty))
              ).flatMap(_.body.asString)
            }.timeoutFail(RpcError(s"IPFS timeout on $gw"))(Duration.fromMillis(timeoutMs))
             .mapError(e => RpcError(s"IPFS fetch failed ($gw): ${e.getMessage}"))
          }
          if attempts.isEmpty then ZIO.fail(RpcError("No IPFS gateways configured"))
          else
            ZIO.raceAll(attempts.head, attempts.tail)
              .mapError(e => RpcError(s"All IPFS gateways failed: ${e.getMessage}"))

        private def parseProfileJson(address: String, json: String): Either[String, Profile] =
          json.fromJson[LspProfileJson].map { p =>
            val data = p.LSP3Profile
            Profile(
              address         = address.toLowerCase,
              name            = data.flatMap(_.name).getOrElse(""),
              description     = data.flatMap(_.description).getOrElse(""),
              tags            = data.flatMap(_.tags).getOrElse(Nil),
              links           = data.flatMap(_.links).getOrElse(Nil).map(l =>
                Profile.Link(l.title.getOrElse(""), l.url.getOrElse(""))),
              avatar          = data.flatMap(_.profileImage).flatMap(_.headOption).flatMap(_.url),
              backgroundImage = data.flatMap(_.backgroundImage).flatMap(_.headOption).flatMap(_.url)
            )
          }
    }

  // Internal JSON models for IPFS LSP3 JSON parsing
  private final case class LspImageEntry(url: Option[String])
  private object LspImageEntry:
    given JsonDecoder[LspImageEntry] = DeriveJsonDecoder.gen

  private final case class LspLinkEntry(title: Option[String], url: Option[String])
  private object LspLinkEntry:
    given JsonDecoder[LspLinkEntry] = DeriveJsonDecoder.gen

  private final case class LspProfileData(
    name:            Option[String],
    description:     Option[String],
    tags:            Option[List[String]],
    links:           Option[List[LspLinkEntry]],
    profileImage:    Option[List[LspImageEntry]],
    backgroundImage: Option[List[LspImageEntry]]
  )
  private object LspProfileData:
    given JsonDecoder[LspProfileData] = DeriveJsonDecoder.gen

  private final case class LspProfileJson(LSP3Profile: Option[LspProfileData])
  private object LspProfileJson:
    given JsonDecoder[LspProfileJson] = DeriveJsonDecoder.gen
