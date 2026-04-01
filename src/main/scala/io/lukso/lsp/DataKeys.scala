package io.lukso.lsp

/** All known ERC725Y data keys for LUKSO LSP standards.
  * These are keccak256 hashes of key names, used with ERC725Y.getData(). */
object DataKeys:

  // LSP3 — Profile Metadata
  val LSP3Profile = "0x5ef83ad9559033e6e941db7d7c495acdce616347d28e90c7ce47cbfcfcad3bc5"

  // LSP4 — Digital Asset Metadata
  val LSP4TokenName   = "0xdeba1e292f8ba88238e10ab3c7f88bd4be4fac56cad5194b6ecceaf653468af1"
  val LSP4TokenSymbol = "0x2f0a68ab07768e01943a599e73362a0e17a63a72e94dd2e384d2c1d4db932756"
  val LSP4TokenType   = "0xe0261fa95db2eb3b5439bd033cda66d56b96f92f243a8228fd87550ed7bdfdb3"
  val LSP4Metadata    = "0x9afb95cacc9f95858ec44aa8c3b685511002e30ae54415823f406128b85b238e"
  val LSP4Creators    = "0x114bd03b3a46d48759680d81ebb2a414fda4d3c706059ee7afd42d98e2fc4f99"

  // LSP5 — Received Assets
  val LSP5ReceivedAssets = "0x6460ee3c0aac563ccbf76d6e1d07bada78e3a9514e6382b736ed3f478ab7b90b"

  // LSP6 — Key Manager Permissions
  val AddressPermissionsPrefix     = "0x4b80742de2bf82acb3630000"
  val AddressPermissionsArray      = "0xdf30dba06db6a30e65354d9a64c609861f089545ca58c6b4dbe31a5f338cb0e3"
  val AddressAllowedCallsPrefix    = "0x4b80742de2bf82acb3630001"

  // LSP10 — Received Vaults
  val LSP10Vaults = "0x55482936e01da86729a45d2b87a6b1d3bc582bea0ec00e38bdb340e3af6f9f06"

  // LSP12 — Issued Assets
  val LSP12IssuedAssets = "0x7c8c3416d6cda87cd42c71ea1843df28ac4850354f988d55ee2eaa47b6dc05cd"

  /** Build the LSP6 permission key for a specific controller address.
    * Pattern: prefix(12 bytes) + address(20 bytes, lowercase, no 0x). */
  def permissionKeyFor(address: String): String =
    val addr = if address.startsWith("0x") then address.drop(2).toLowerCase else address.toLowerCase
    AddressPermissionsPrefix + addr

  /** Build the LSP6 AllowedCalls key for a specific controller address. */
  def allowedCallsKeyFor(address: String): String =
    val addr = if address.startsWith("0x") then address.drop(2).toLowerCase else address.toLowerCase
    AddressAllowedCallsPrefix + addr

  /** Build an LSP2 Array element key.
    * Base key first 16 bytes + uint128 index (16 bytes). */
  def arrayElementKey(baseKey: String, index: Int): String =
    val base = baseKey.stripPrefix("0x")
    val indexHex = "%032x".format(index)
    "0x" + base.take(32) + indexHex
