package io.lukso.models

import zio.json.*

object Transaction:
  final case class Decoded(
    hash:        String,
    from:        String,
    to:          Option[String],
    value:       String,
    status:      String,
    selector:    String,
    method:      String,
    lspContext:   String,
    events:      List[DecodedEvent],
    gasUsed:     String,
    blockNumber: Option[Long],
    nonce:       Long
  )
  object Decoded:
    given JsonEncoder[Decoded] = DeriveJsonEncoder.gen
    given JsonDecoder[Decoded] = DeriveJsonDecoder.gen

  final case class DecodedEvent(name: String, data: String)
  object DecodedEvent:
    given JsonEncoder[DecodedEvent] = DeriveJsonEncoder.gen
    given JsonDecoder[DecodedEvent] = DeriveJsonDecoder.gen
