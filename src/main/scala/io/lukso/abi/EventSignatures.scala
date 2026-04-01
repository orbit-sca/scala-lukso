package io.lukso.abi

/** Known event topic0 hashes for LUKSO and ERC standard events. */
object EventSignatures:
  val known: Map[String, String] = Map(
    "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef" -> "Transfer(address,address,uint256)",
    "0x8c5be1e5ebec7d5bd14f71427d1e84f3dd0314c0f7b2291e5b200ac8c7c3b925" -> "Approval(address,address,uint256)",
    "0x6bb7ff708619ba0610cba295a58592e0451dee2622938c8755667688daf3529b" -> "DataChanged(bytes32,bytes)",
    "0x3f4dcec6b4fa4c9e0c5e81f05f58eb9c1d62d8a2f7efb4cd8b0e8b4ac6e9a52" -> "UniversalReceiver(address,uint256,bytes32,bytes,bytes)",
    "0xb3d987963514436e22a8e491c948e35e59575fa525679da9233c6c0ba1a22e11" -> "OwnershipTransferStarted(address,address)",
    "0x8be0079c531659141344cd1fd0a4f28419497f9722a3daafe3b4186f6b6457e0" -> "OwnershipTransferred(address,address)",
  )

  /** Look up an event by topic0 hash. */
  def lookup(topic0: String): Option[String] =
    known.get(topic0.toLowerCase)
