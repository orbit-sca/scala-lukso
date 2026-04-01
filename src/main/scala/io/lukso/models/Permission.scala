package io.lukso.models

import zio.json.*

/** LSP6 Key Manager Permission flags and bitmask decoding. */
object Permission:

  /** All 22 LSP6 permission flags with their bitmask values.
    * Source: LUKSO LSP6 specification + @lukso/lsp-smart-contracts. */
  val allFlags: Map[String, BigInt] = Map(
    "CHANGEOWNER"                     -> BigInt("01", 16),
    "ADDCONTROLLER"                   -> BigInt("02", 16),
    "EDITPERMISSIONS"                 -> BigInt("04", 16),
    "ADDEXTENSIONS"                   -> BigInt("08", 16),
    "CHANGEEXTENSIONS"                -> BigInt("10", 16),
    "ADDUNIVERSALRECEIVERDELEGATE"    -> BigInt("20", 16),
    "CHANGEUNIVERSALRECEIVERDELEGATE" -> BigInt("40", 16),
    "REENTRANCY"                      -> BigInt("80", 16),
    "SUPER_TRANSFERVALUE"             -> BigInt("100", 16),
    "TRANSFERVALUE"                   -> BigInt("200", 16),
    "SUPER_CALL"                      -> BigInt("400", 16),
    "CALL"                            -> BigInt("800", 16),
    "SUPER_STATICCALL"                -> BigInt("1000", 16),
    "STATICCALL"                      -> BigInt("2000", 16),
    "SUPER_DELEGATECALL"              -> BigInt("4000", 16),
    "DELEGATECALL"                    -> BigInt("8000", 16),
    "DEPLOY"                          -> BigInt("10000", 16),
    "SUPER_SETDATA"                   -> BigInt("20000", 16),
    "SETDATA"                         -> BigInt("40000", 16),
    "ENCRYPT"                         -> BigInt("80000", 16),
    "DECRYPT"                         -> BigInt("100000", 16),
    "SIGN"                            -> BigInt("200000", 16),
    "EXECUTE_RELAY_CALL"              -> BigInt("400000", 16)
  )

  /** Decode a raw bitmask into named flag booleans. */
  def decode(raw: BigInt): Map[String, Boolean] =
    allFlags.map { case (name, bit) =>
      name -> ((raw & bit) != BigInt(0))
    }

  /** Decoded permissions result. */
  final case class Decoded(
    raw:     String,
    flags:   Map[String, Boolean],
    granted: Int
  )
  object Decoded:
    given JsonEncoder[Decoded] = DeriveJsonEncoder.gen
    given JsonDecoder[Decoded] = DeriveJsonDecoder.gen
