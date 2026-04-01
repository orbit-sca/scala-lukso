package io.lukso.crypto

import zio.test.*

object Keccak256Spec extends ZIOSpecDefault:
  def spec = suite("Keccak256")(
    test("hashString produces correct hash for LSP3Profile key") {
      val result = Keccak256.hashString("LSP3Profile")
      assertTrue(result == "0x5ef83ad9559033e6e941db7d7c495acdce616347d28e90c7ce47cbfcfcad3bc5")
    },
    test("hashString produces correct hash for LSP4TokenName key") {
      val result = Keccak256.hashString("LSP4TokenName")
      assertTrue(result == "0xdeba1e292f8ba88238e10ab3c7f88bd4be4fac56cad5194b6ecceaf653468af1")
    },
    test("hashString produces correct hash for LSP4TokenSymbol key") {
      val result = Keccak256.hashString("LSP4TokenSymbol")
      assertTrue(result == "0x2f0a68ab07768e01943a599e73362a0e17a63a72e94dd2e384d2c1d4db932756")
    },
    test("hashString produces correct hash for LSP5ReceivedAssets[] key") {
      val result = Keccak256.hashString("LSP5ReceivedAssets[]")
      assertTrue(result == "0x6460ee3c0aac563ccbf76d6e1d07bada78e3a9514e6382b736ed3f478ab7b90b")
    },
    test("hashString produces correct hash for LSP12IssuedAssets[] key") {
      val result = Keccak256.hashString("LSP12IssuedAssets[]")
      assertTrue(result == "0x7c8c3416d6cda87cd42c71ea1843df28ac4850354f988d55ee2eaa47b6dc05cd")
    },
    test("hexToBytes and bytesToHex are inverse") {
      val original = "0xdeadbeef"
      val bytes = Keccak256.hexToBytes(original)
      val roundtrip = Keccak256.bytesToHex(bytes)
      assertTrue(roundtrip == original)
    },
    test("empty input hashes correctly") {
      val result = Keccak256.hashString("")
      assertTrue(result.startsWith("0x") && result.length == 66) // 0x + 64 hex chars
    }
  )
