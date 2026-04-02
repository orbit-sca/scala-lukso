package io.lukso

final case class LuksoConfig(
  rpcEndpoints:   List[String]        = LuksoConfig.defaultRpcEndpoints,
  rpcHeaders:     Map[String, String] = Map.empty,
  chainId:        Int                 = 42,
  ipfsGateways:   List[String]        = LuksoConfig.defaultIpfsGateways,
  envioEndpoint:  String              = "https://envio.lukso-mainnet.universal.tech/v1/graphql",
  blockscoutBase: String              = "https://explorer.execution.mainnet.lukso.network/api/v2",
  rpcTimeoutMs:   Long                = 8000,
  ipfsTimeoutMs:  Long                = 10000
)

object LuksoConfig:
  val defaultRpcEndpoints: List[String] = List(
    "https://rpc.mainnet.lukso.network",
    "https://42.rpc.thirdweb.com",
    "https://lukso.drpc.org"
  )

  val defaultIpfsGateways: List[String] = List(
    "https://api.universalprofile.cloud/ipfs/",
    "https://ipfs.io/ipfs/",
    "https://cloudflare-ipfs.com/ipfs/"
  )

  val default: LuksoConfig = LuksoConfig()
