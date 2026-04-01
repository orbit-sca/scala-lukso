package io.lukso.lsp

import io.lukso.erc.ERC725Y
import io.lukso.rpc.RpcError
import zio.*

trait LSP12:
  /** Read all issued asset contract addresses from a UP's LSP12IssuedAssets[] array. */
  def getIssuedAssets(upAddress: String): IO[RpcError, List[String]]

object LSP12:
  def getIssuedAssets(upAddress: String): ZIO[LSP12, RpcError, List[String]] =
    ZIO.serviceWithZIO(_.getIssuedAssets(upAddress))

  val layer: ZLayer[ERC725Y, Nothing, LSP12] =
    ZLayer.fromFunction { (erc725y: ERC725Y) =>
      new LSP12:
        override def getIssuedAssets(upAddress: String): IO[RpcError, List[String]] =
          for
            lengthHex <- erc725y.getData(upAddress, DataKeys.LSP12IssuedAssets)
            count      = parseUint128Length(lengthHex)
            indexKeys  = (0 until count).toList.map(i => DataKeys.arrayElementKey(DataKeys.LSP12IssuedAssets, i))
            values    <- if indexKeys.isEmpty then ZIO.succeed(List.empty[String])
                         else ZIO.foreach(indexKeys)(key => erc725y.getData(upAddress, key))
          yield values.map(extractAddress).filter(_.nonEmpty)

        private def parseUint128Length(hex: String): Int =
          val clean = hex.stripPrefix("0x").replaceAll("^0+", "")
          if clean.isEmpty then 0
          else scala.util.Try(Integer.parseInt(clean.take(8), 16)).getOrElse(0)

        private def extractAddress(hex: String): String =
          val clean = hex.stripPrefix("0x").replaceAll("^0+", "")
          if clean.length >= 40 then "0x" + clean.takeRight(40).toLowerCase else ""
    }
