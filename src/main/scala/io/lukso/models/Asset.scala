package io.lukso.models

import zio.json.*

object Asset:
  /** LSP4 asset metadata. */
  final case class Metadata(
    name:        String,
    symbol:      String,
    tokenType:   Int,       // 0=Token, 1=NFT, 2=Collection
    decimals:    Int,
    totalSupply: BigInt
  )
  object Metadata:
    given JsonEncoder[Metadata] = DeriveJsonEncoder.gen
    given JsonDecoder[Metadata] = DeriveJsonDecoder.gen

  enum TokenType(val code: Int):
    case Token      extends TokenType(0)
    case NFT        extends TokenType(1)
    case Collection extends TokenType(2)

  object TokenType:
    def fromCode(code: Int): TokenType = code match
      case 0 => TokenType.Token
      case 1 => TokenType.NFT
      case 2 => TokenType.Collection
      case _ => TokenType.Token
