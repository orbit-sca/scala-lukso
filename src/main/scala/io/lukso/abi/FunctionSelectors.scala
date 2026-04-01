package io.lukso.abi

/** Known 4-byte function selectors for LUKSO contract methods.
  * Used for transaction decoding. */
object FunctionSelectors:
  final case class KnownMethod(signature: String, lspContext: String)

  val known: Map[String, KnownMethod] = Map(
    // LSP7 — Digital Asset (Fungible Token)
    "0x760d9bba" -> KnownMethod("transfer(address,address,uint256,bool,bytes)", "LSP7 token transfer"),
    "0x40c10f19" -> KnownMethod("mint(address,uint256,bool,bytes)", "LSP7 mint"),
    "0x58b4e4af" -> KnownMethod("burn(address,uint256,bytes)", "LSP7 burn"),
    "0x3a22f5e4" -> KnownMethod("authorizeOperator(address,uint256,bytes)", "LSP7 operator authorization"),
    "0x5765a5cc" -> KnownMethod("revokeOperator(address,bool,bytes)", "LSP7 operator revocation"),

    // LSP8 — Identifiable Digital Asset (NFT)
    "0x511b6952" -> KnownMethod("transfer(address,address,bytes32,bool,bytes)", "LSP8 NFT transfer"),
    "0xc3ade485" -> KnownMethod("mint(address,bytes32,bool,bytes)", "LSP8 mint"),
    "0xe22a2b24" -> KnownMethod("authorizeOperator(address,bytes32,bool,bytes)", "LSP8 operator authorization"),

    // LSP0 / ERC725X — Universal Profile Execute
    "0x7f23690c" -> KnownMethod("execute(uint256,address,uint256,bytes)", "LSP0/LSP9 execute call"),
    "0x31858452" -> KnownMethod("executeBatch(uint256[],address[],uint256[],bytes[])", "LSP0/LSP9 batch execute"),

    // ERC725Y — Data Store
    "0x14d67462" -> KnownMethod("setData(bytes32,bytes)", "ERC725Y data write"),
    "0x97902421" -> KnownMethod("setDataBatch(bytes32[],bytes[])", "ERC725Y batch data write"),
    "0x54f6127f" -> KnownMethod("getData(bytes32)", "ERC725Y data read"),
    "0xdedff9c6" -> KnownMethod("getDataBatch(bytes32[])", "ERC725Y batch data read"),

    // LSP1 — Universal Receiver
    "0x44c028fe" -> KnownMethod("universalReceiver(bytes32,bytes)", "LSP1 universal receiver"),

    // LSP14 — Ownable 2-Step
    "0x9afd3db0" -> KnownMethod("transferOwnership(address)", "LSP14 ownership transfer initiation"),
    "0x6d5b6174" -> KnownMethod("claimOwnership()", "LSP14 ownership transfer acceptance"),
    "0xee3e35ab" -> KnownMethod("renounceOwnership()", "LSP14 ownership renouncement"),

    // LSP25 — Execute Relay Call
    "0x2fb1b25a" -> KnownMethod("executeRelayCall(bytes,uint256,uint256,bytes)", "LSP25 meta-transaction relay"),

    // LSP6 — Key Manager
    "0x1fad948c" -> KnownMethod("execute(bytes)", "LSP6 Key Manager execute"),
    "0xbf0176ff" -> KnownMethod("executeBatch(uint256[],bytes[])", "LSP6 Key Manager batch execute"),

    // ERC20 compatibility
    "0xa9059cbb" -> KnownMethod("transfer(address,uint256)", "ERC20 transfer"),
    "0x23b872dd" -> KnownMethod("transferFrom(address,address,uint256)", "ERC20 transferFrom"),
    "0x095ea7b3" -> KnownMethod("approve(address,uint256)", "ERC20 approve"),

    // ERC165
    "0x01ffc9a7" -> KnownMethod("supportsInterface(bytes4)", "ERC165 interface check")
  )

  /** Look up a 4-byte selector. Returns None if unknown. */
  def lookup(selector: String): Option[KnownMethod] =
    val clean = selector.toLowerCase.take(10)
    known.get(clean)
