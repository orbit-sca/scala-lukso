package io.lukso.models

import zio.json.*

opaque type Address = String

object Address:
  def apply(raw: String): Either[String, Address] =
    val clean = raw.trim.toLowerCase
    if clean.matches("^0x[0-9a-f]{40}$") then Right(clean)
    else Left(s"Invalid address: $raw")

  def unsafe(raw: String): Address = raw.trim.toLowerCase

  extension (a: Address)
    def value: String = a
    def short: String = s"${a.take(6)}...${a.takeRight(4)}"

  given JsonEncoder[Address] = JsonEncoder.string.contramap(identity)
  given JsonDecoder[Address] = JsonDecoder.string.mapOrFail(apply)
