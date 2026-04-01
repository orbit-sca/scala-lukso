package io.lukso.indexer

import io.lukso.LuksoConfig
import zio.*
import zio.http.*
import zio.json.*
import zio.json.ast.Json

case class EnvioError(message: String) extends Exception(message)

trait EnvioClient:
  /** Search profiles by name. */
  def searchProfiles(query: String, limit: Int = 10): IO[EnvioError, List[EnvioClient.ProfileResult]]

  /** Get profile by address (name, images). */
  def getProfile(address: String): IO[EnvioError, Option[EnvioClient.EnvioProfile]]

  /** Get followers/following for an address. */
  def getFollowGraph(address: String, limit: Int = 20): IO[EnvioError, EnvioClient.FollowGraphResult]

  /** Get received assets with LSP4TokenType classification. */
  def getReceivedAssetsClassified(address: String): IO[EnvioError, List[EnvioClient.ClassifiedAsset]]

  /** Get controller permissions from Envio index. */
  def getControllers(address: String): IO[EnvioError, List[EnvioClient.ControllerInfo]]

object EnvioClient:
  final case class ProfileResult(address: String, name: Option[String])
  object ProfileResult:
    given JsonDecoder[ProfileResult] = DeriveJsonDecoder.gen
    given JsonEncoder[ProfileResult] = DeriveJsonEncoder.gen

  final case class EnvioProfile(name: Option[String], avatar: Option[String], backgroundImage: Option[String])
  object EnvioProfile:
    given JsonDecoder[EnvioProfile] = DeriveJsonDecoder.gen

  final case class FollowGraphResult(
    followersCount: Int,
    followingCount: Int,
    followers:      List[String],
    following:      List[String]
  )
  object FollowGraphResult:
    given JsonDecoder[FollowGraphResult] = DeriveJsonDecoder.gen
    given JsonEncoder[FollowGraphResult] = DeriveJsonEncoder.gen

  final case class ClassifiedAsset(
    address:   String,
    name:      String,
    symbol:    String,
    tokenType: Int,
    isLSP7:    Boolean,
    decimals:  Int
  )
  object ClassifiedAsset:
    given JsonDecoder[ClassifiedAsset] = DeriveJsonDecoder.gen
    given JsonEncoder[ClassifiedAsset] = DeriveJsonEncoder.gen

  final case class ControllerInfo(address: String, permissions: String, blockNumber: Long)
  object ControllerInfo:
    given JsonDecoder[ControllerInfo] = DeriveJsonDecoder.gen
    given JsonEncoder[ControllerInfo] = DeriveJsonEncoder.gen

  // Service accessors
  def searchProfiles(query: String): ZIO[EnvioClient, EnvioError, List[ProfileResult]] =
    ZIO.serviceWithZIO(_.searchProfiles(query))

  def getProfile(address: String): ZIO[EnvioClient, EnvioError, Option[EnvioProfile]] =
    ZIO.serviceWithZIO(_.getProfile(address))

  def getFollowGraph(address: String): ZIO[EnvioClient, EnvioError, FollowGraphResult] =
    ZIO.serviceWithZIO(_.getFollowGraph(address))

  // GraphQL queries
  private val SEARCH_QUERY = """query SearchProfiles($search: String!) {
    search_profiles(args: { search: $search }) { id name }
  }"""

  private val PROFILE_QUERY = """query GetProfile($addr: String!) {
    Profile(where: { id: { _eq: $addr } }) { name profileImages { url } backgroundImages { url } }
  }"""

  private val FOLLOW_QUERY = """query GetFollowGraph($addr: String!) {
    followers: Follow(where: { followee_id: { _eq: $addr } } limit: 50 order_by: { blockNumber: desc }) { follower_id }
    following: Follow(where: { follower_id: { _eq: $addr } } limit: 50 order_by: { blockNumber: desc }) { followee_id }
    followersCount: Follow_aggregate(where: { followee_id: { _eq: $addr } }) { aggregate { count } }
    followingCount: Follow_aggregate(where: { follower_id: { _eq: $addr } }) { aggregate { count } }
  }"""

  private val ASSETS_QUERY = """query GetAssets($addr: String!) {
    Profile(where: { id: { _eq: $addr } }) {
      lsp5ReceivedAssets(limit: 200) {
        asset { id lsp4TokenName lsp4TokenSymbol lsp4TokenType decimals isLSP7 }
      }
    }
  }"""

  private val PERMISSIONS_QUERY = """query GetPermissions($addr: String!) {
    Profile(where: {id: {_eq: $addr}}) { id owner_id controllers { address permissions blockNumber } }
  }"""

  val layer: ZLayer[Client & LuksoConfig, Nothing, EnvioClient] =
    ZLayer.fromFunction { (httpClient: Client, config: LuksoConfig) =>
      new EnvioClient:
        private def graphql(query: String, variables: Json.Obj): IO[EnvioError, Json] =
          val body = Json.Obj("query" -> Json.Str(query), "variables" -> variables)
          (for
            url  <- ZIO.fromEither(URL.decode(config.envioEndpoint)).mapError(e => EnvioError(s"Bad URL: $e"))
            resp <- ZIO.scoped {
                      httpClient.request(
                        Request.post(url, Body.fromString(body.toJson))
                          .addHeader(Header.ContentType(MediaType.application.json))
                      ).mapError(e => EnvioError(s"HTTP error: ${e.getMessage}"))
                    }
            text <- resp.body.asString.mapError(e => EnvioError(s"Body read error: ${e.getMessage}"))
            json <- ZIO.fromEither(text.fromJson[Json]).mapError(e => EnvioError(s"JSON parse: $e"))
          yield json).timeoutFail(EnvioError("Envio timeout"))(Duration.fromMillis(10000))

        private def extractData(json: Json): Option[Json] =
          json match
            case Json.Obj(fields) => fields.collectFirst { case ("data", v) => v }
            case _ => None

        override def searchProfiles(query: String, limit: Int): IO[EnvioError, List[ProfileResult]] =
          graphql(SEARCH_QUERY, Json.Obj("search" -> Json.Str(query))).map { json =>
            extractData(json).flatMap { data =>
              data.toJson.fromJson[SearchResult].toOption.map { sr =>
                sr.search_profiles.take(limit).map(p => ProfileResult(p.id, p.name))
              }
            }.getOrElse(Nil)
          }

        override def getProfile(address: String): IO[EnvioError, Option[EnvioProfile]] =
          graphql(PROFILE_QUERY, Json.Obj("addr" -> Json.Str(address.toLowerCase))).map { json =>
            extractData(json).flatMap { data =>
              data.toJson.fromJson[ProfileQueryResult].toOption.flatMap { pqr =>
                pqr.Profile.headOption.map { p =>
                  EnvioProfile(
                    name = p.name,
                    avatar = p.profileImages.flatMap(_.headOption).flatMap(_.url),
                    backgroundImage = p.backgroundImages.flatMap(_.headOption).flatMap(_.url)
                  )
                }
              }
            }
          }

        override def getFollowGraph(address: String, limit: Int): IO[EnvioError, FollowGraphResult] =
          graphql(FOLLOW_QUERY, Json.Obj("addr" -> Json.Str(address.toLowerCase))).map { json =>
            extractData(json).flatMap { data =>
              data.toJson.fromJson[FollowQueryResult].toOption.map { fqr =>
                FollowGraphResult(
                  followersCount = fqr.followersCount.flatMap(_.aggregate).map(_.count).getOrElse(0),
                  followingCount = fqr.followingCount.flatMap(_.aggregate).map(_.count).getOrElse(0),
                  followers = fqr.followers.getOrElse(Nil).flatMap(_.follower_id).take(limit),
                  following = fqr.following.getOrElse(Nil).flatMap(_.followee_id).take(limit)
                )
              }
            }.getOrElse(FollowGraphResult(0, 0, Nil, Nil))
          }

        override def getReceivedAssetsClassified(address: String): IO[EnvioError, List[ClassifiedAsset]] =
          graphql(ASSETS_QUERY, Json.Obj("addr" -> Json.Str(address.toLowerCase))).map { json =>
            extractData(json).flatMap { data =>
              data.toJson.fromJson[AssetsQueryResult].toOption.flatMap { aqr =>
                aqr.Profile.headOption.map { p =>
                  p.lsp5ReceivedAssets.getOrElse(Nil).flatMap { ra =>
                    ra.asset.map { a =>
                      ClassifiedAsset(
                        address = a.id,
                        name = a.lsp4TokenName.getOrElse(""),
                        symbol = a.lsp4TokenSymbol.getOrElse(""),
                        tokenType = a.lsp4TokenType.getOrElse(0),
                        isLSP7 = a.isLSP7.getOrElse(true),
                        decimals = a.decimals.getOrElse(18)
                      )
                    }
                  }
                }
              }
            }.getOrElse(Nil)
          }

        override def getControllers(address: String): IO[EnvioError, List[ControllerInfo]] =
          graphql(PERMISSIONS_QUERY, Json.Obj("addr" -> Json.Str(address.toLowerCase))).map { json =>
            extractData(json).flatMap { data =>
              data.toJson.fromJson[PermissionsQueryResult].toOption.flatMap { pqr =>
                pqr.Profile.headOption.map { p =>
                  p.controllers.getOrElse(Nil).map { c =>
                    ControllerInfo(c.address, c.permissions.getOrElse("0x"), c.blockNumber.getOrElse(0L))
                  }
                }
              }
            }.getOrElse(Nil)
          }
    }

  // Internal JSON models for Envio GraphQL responses
  private final case class SearchProfileEntry(id: String, name: Option[String])
  private object SearchProfileEntry:
    given JsonDecoder[SearchProfileEntry] = DeriveJsonDecoder.gen
  private final case class SearchResult(search_profiles: List[SearchProfileEntry])
  private object SearchResult:
    given JsonDecoder[SearchResult] = DeriveJsonDecoder.gen

  private final case class ImageEntry(url: Option[String])
  private object ImageEntry:
    given JsonDecoder[ImageEntry] = DeriveJsonDecoder.gen
  private final case class ProfileEntry(name: Option[String], profileImages: Option[List[ImageEntry]], backgroundImages: Option[List[ImageEntry]])
  private object ProfileEntry:
    given JsonDecoder[ProfileEntry] = DeriveJsonDecoder.gen
  private final case class ProfileQueryResult(Profile: List[ProfileEntry])
  private object ProfileQueryResult:
    given JsonDecoder[ProfileQueryResult] = DeriveJsonDecoder.gen

  private final case class FollowerEntry(follower_id: Option[String])
  private object FollowerEntry:
    given JsonDecoder[FollowerEntry] = DeriveJsonDecoder.gen
  private final case class FollowingEntry(followee_id: Option[String])
  private object FollowingEntry:
    given JsonDecoder[FollowingEntry] = DeriveJsonDecoder.gen
  private final case class AggCount(count: Int)
  private object AggCount:
    given JsonDecoder[AggCount] = DeriveJsonDecoder.gen
  private final case class AggWrapper(aggregate: Option[AggCount])
  private object AggWrapper:
    given JsonDecoder[AggWrapper] = DeriveJsonDecoder.gen
  private final case class FollowQueryResult(
    followers: Option[List[FollowerEntry]], following: Option[List[FollowingEntry]],
    followersCount: Option[AggWrapper], followingCount: Option[AggWrapper]
  )
  private object FollowQueryResult:
    given JsonDecoder[FollowQueryResult] = DeriveJsonDecoder.gen

  private final case class AssetEntry(id: String, lsp4TokenName: Option[String], lsp4TokenSymbol: Option[String], lsp4TokenType: Option[Int], decimals: Option[Int], isLSP7: Option[Boolean])
  private object AssetEntry:
    given JsonDecoder[AssetEntry] = DeriveJsonDecoder.gen
  private final case class ReceivedAssetWrapper(asset: Option[AssetEntry])
  private object ReceivedAssetWrapper:
    given JsonDecoder[ReceivedAssetWrapper] = DeriveJsonDecoder.gen
  private final case class AssetsProfileEntry(lsp5ReceivedAssets: Option[List[ReceivedAssetWrapper]])
  private object AssetsProfileEntry:
    given JsonDecoder[AssetsProfileEntry] = DeriveJsonDecoder.gen
  private final case class AssetsQueryResult(Profile: List[AssetsProfileEntry])
  private object AssetsQueryResult:
    given JsonDecoder[AssetsQueryResult] = DeriveJsonDecoder.gen

  private final case class ControllerEntry(address: String, permissions: Option[String], blockNumber: Option[Long])
  private object ControllerEntry:
    given JsonDecoder[ControllerEntry] = DeriveJsonDecoder.gen
  private final case class PermProfileEntry(controllers: Option[List[ControllerEntry]])
  private object PermProfileEntry:
    given JsonDecoder[PermProfileEntry] = DeriveJsonDecoder.gen
  private final case class PermissionsQueryResult(Profile: List[PermProfileEntry])
  private object PermissionsQueryResult:
    given JsonDecoder[PermissionsQueryResult] = DeriveJsonDecoder.gen
