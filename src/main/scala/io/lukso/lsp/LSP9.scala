package io.lukso.lsp

import io.lukso.erc.ERC165
import io.lukso.rpc.RpcError
import zio.*

trait LSP9:
  /** Check if an address is an LSP9 Vault. */
  def isVault(address: String): IO[RpcError, Boolean]

object LSP9:
  def isVault(address: String): ZIO[LSP9, RpcError, Boolean] =
    ZIO.serviceWithZIO(_.isVault(address))

  val layer: ZLayer[ERC165, Nothing, LSP9] =
    ZLayer.fromFunction { (erc165: ERC165) =>
      new LSP9:
        override def isVault(address: String): IO[RpcError, Boolean] =
          erc165.supportsInterface(address, InterfaceIds.LSP9)
    }
