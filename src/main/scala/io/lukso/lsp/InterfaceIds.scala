package io.lukso.lsp

/** ERC165 interface IDs for LUKSO standards.
  *
  * IMPORTANT: LSP6 and LSP7 IDs are hardcoded to values deployed on LUKSO mainnet,
  * which differ from what the @lukso/lsp-smart-contracts npm package ships. */
object InterfaceIds:
  val LSP0   = "0x24871b3d"  // ERC725Account (Universal Profile)
  val LSP6   = "0x23965e9c"  // Key Manager (HARDCODED: mainnet differs from npm)
  val LSP7   = "0xda1f85e4"  // Digital Asset / Fungible Token (HARDCODED: mainnet differs)
  val LSP8   = "0x3a271706"  // Identifiable Digital Asset / NFT
  val LSP9   = "0x28af17e6"  // Vault
  val ERC20  = "0x36372b07"
  val ERC721 = "0x80ac58cd"
  val ERC165 = "0x01ffc9a7"

  /** All standard interfaces for contract type detection. */
  val allStandard: Map[String, String] = Map(
    "LSP0"   -> LSP0,
    "LSP6"   -> LSP6,
    "LSP7"   -> LSP7,
    "LSP8"   -> LSP8,
    "LSP9"   -> LSP9,
    "ERC20"  -> ERC20,
    "ERC721" -> ERC721
  )
