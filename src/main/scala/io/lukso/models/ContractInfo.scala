package io.lukso.models

import zio.json.*

final case class ContractInfo(
  address:           String,
  isContract:        Boolean,
  contractType:      String,             // "UniversalProfile" | "KeyManager" | "LSP7Token" | "LSP8Collection" | "Vault" | "EOA" | "UnknownContract"
  detectedStandards: List[String],
  interfaceIds:      Map[String, Boolean],
  tokenInfo:         Option[Asset.Metadata] = None
)

object ContractInfo:
  given JsonEncoder[ContractInfo] = DeriveJsonEncoder.gen
  given JsonDecoder[ContractInfo] = DeriveJsonDecoder.gen
