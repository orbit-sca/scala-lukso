package io.lukso.models

import zio.json.*

final case class Profile(
  address:         String,
  name:            String,
  description:     String,
  tags:            List[String]       = Nil,
  links:           List[Profile.Link] = Nil,
  avatar:          Option[String]     = None,
  backgroundImage: Option[String]     = None
)

object Profile:
  final case class Link(title: String, url: String)
  object Link:
    given JsonEncoder[Link] = DeriveJsonEncoder.gen
    given JsonDecoder[Link] = DeriveJsonDecoder.gen

  given JsonEncoder[Profile] = DeriveJsonEncoder.gen
  given JsonDecoder[Profile] = DeriveJsonDecoder.gen
