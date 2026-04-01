package io.lukso.lsp

import io.lukso.abi.AbiCodec
import io.lukso.erc.ERC725Y
import io.lukso.models.Permission
import io.lukso.rpc.{JsonRpcClient, RpcError}
import zio.*

trait LSP6:
  /** Get decoded permissions for a specific controller on a UP. */
  def getPermissions(upAddress: String, controllerAddress: String): IO[RpcError, Permission.Decoded]

  /** Get the owner (Key Manager address) of a UP. */
  def getKeyManager(upAddress: String): IO[RpcError, String]

  /** Check if a controller has a specific permission. */
  def hasPermission(upAddress: String, controllerAddress: String, permissionName: String): IO[RpcError, Boolean]

object LSP6:
  def getPermissions(upAddress: String, controllerAddress: String): ZIO[LSP6, RpcError, Permission.Decoded] =
    ZIO.serviceWithZIO(_.getPermissions(upAddress, controllerAddress))

  def getKeyManager(upAddress: String): ZIO[LSP6, RpcError, String] =
    ZIO.serviceWithZIO(_.getKeyManager(upAddress))

  def hasPermission(upAddress: String, controllerAddress: String, permissionName: String): ZIO[LSP6, RpcError, Boolean] =
    ZIO.serviceWithZIO(_.hasPermission(upAddress, controllerAddress, permissionName))

  val layer: ZLayer[ERC725Y & JsonRpcClient, Nothing, LSP6] =
    ZLayer.fromFunction { (erc725y: ERC725Y, rpc: JsonRpcClient) =>
      new LSP6:
        override def getPermissions(upAddress: String, controllerAddress: String): IO[RpcError, Permission.Decoded] =
          val key = DataKeys.permissionKeyFor(controllerAddress)
          erc725y.getData(upAddress, key).map { rawHex =>
            val raw = if rawHex == "0x" || rawHex.length < 4 then BigInt(0)
                      else BigInt(rawHex.stripPrefix("0x"), 16)
            val flags = Permission.decode(raw)
            Permission.Decoded(
              raw     = rawHex,
              flags   = flags,
              granted = flags.count(_._2)
            )
          }

        override def getKeyManager(upAddress: String): IO[RpcError, String] =
          val calldata = AbiCodec.encodeOwner()
          rpc.ethCall(upAddress, calldata).map(AbiCodec.decodeAddress)

        override def hasPermission(upAddress: String, controllerAddress: String, permissionName: String): IO[RpcError, Boolean] =
          getPermissions(upAddress, controllerAddress).map(_.flags.getOrElse(permissionName, false))
    }
