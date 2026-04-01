package io.lukso.erc

import io.lukso.abi.AbiCodec
import io.lukso.crypto.Keccak256
import io.lukso.rpc.{JsonRpcClient, RpcError}
import zio.*

trait ERC725Y:
  /** Read a single ERC725Y data value by bytes32 key. Returns raw hex. */
  def getData(contractAddress: String, dataKey: String): IO[RpcError, String]

  /** Read multiple ERC725Y data values. Returns list of raw hex values. */
  def getDataBatch(contractAddress: String, dataKeys: List[String]): IO[RpcError, List[String]]

object ERC725Y:
  def getData(contractAddress: String, dataKey: String): ZIO[ERC725Y, RpcError, String] =
    ZIO.serviceWithZIO(_.getData(contractAddress, dataKey))

  def getDataBatch(contractAddress: String, dataKeys: List[String]): ZIO[ERC725Y, RpcError, List[String]] =
    ZIO.serviceWithZIO(_.getDataBatch(contractAddress, dataKeys))

  val layer: ZLayer[JsonRpcClient, Nothing, ERC725Y] =
    ZLayer.fromFunction { (rpc: JsonRpcClient) =>
      new ERC725Y:
        override def getData(contractAddress: String, dataKey: String): IO[RpcError, String] =
          val calldata = AbiCodec.encodeGetData(dataKey)
          rpc.ethCall(contractAddress, calldata).map { hex =>
            decodeReturnedBytes(hex)
          }

        override def getDataBatch(contractAddress: String, dataKeys: List[String]): IO[RpcError, List[String]] =
          // Use individual calls for reliability (some RPCs have issues with batch)
          ZIO.foreach(dataKeys)(key => getData(contractAddress, key))

        /** Decode ABI-encoded bytes return value.
          * Format: offset(32 bytes) + length(32 bytes) + data */
        private def decodeReturnedBytes(hex: String): String =
          val clean = hex.stripPrefix("0x")
          if clean.length < 128 then "0x"
          else
            val lengthHex = clean.slice(64, 128)
            val length = try {
              java.lang.Long.parseLong(lengthHex.replaceAll("^0+", "").nn.trim.nn, 16).toInt * 2
            } catch {
              case _: NumberFormatException => 0
            }
            if length == 0 then "0x"
            else "0x" + clean.slice(128, math.min(128 + length, clean.length))
    }
