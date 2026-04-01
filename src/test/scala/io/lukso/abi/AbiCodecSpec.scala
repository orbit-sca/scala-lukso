package io.lukso.abi

import zio.test.*

object AbiCodecSpec extends ZIOSpecDefault:
  def spec = suite("AbiCodec")(
    test("encodeSupportsInterface produces valid calldata") {
      val calldata = AbiCodec.encodeSupportsInterface("0x24871b3d")
      // Should start with the supportsInterface selector: 0x01ffc9a7
      assertTrue(calldata.startsWith("0x01ffc9a7"))
    },
    test("encodeGetData produces valid calldata") {
      val calldata = AbiCodec.encodeGetData("0x5ef83ad9559033e6e941db7d7c495acdce616347d28e90c7ce47cbfcfcad3bc5")
      // Should start with the getData selector: 0x54f6127f
      assertTrue(calldata.startsWith("0x54f6127f"))
    },
    test("encodeOwner produces valid calldata") {
      val calldata = AbiCodec.encodeOwner()
      // owner() selector: 0x8da5cb5b
      assertTrue(calldata.startsWith("0x8da5cb5b"))
    },
    test("encodeBalanceOf produces valid calldata") {
      val calldata = AbiCodec.encodeBalanceOf("0xEb2Cb5e155AC36C2d50BE9750bFcAb6fc1f3a607")
      // balanceOf(address) selector: 0x70a08231
      assertTrue(calldata.startsWith("0x70a08231"))
    },
    test("encodeName produces valid calldata") {
      val calldata = AbiCodec.encodeName()
      // name() selector: 0x06fdde03
      assertTrue(calldata.startsWith("0x06fdde03"))
    },
    test("decodeBool handles true") {
      val hex = "0x0000000000000000000000000000000000000000000000000000000000000001"
      assertTrue(AbiCodec.decodeBool(hex) == true)
    },
    test("decodeBool handles false") {
      val hex = "0x0000000000000000000000000000000000000000000000000000000000000000"
      assertTrue(AbiCodec.decodeBool(hex) == false)
    },
    test("decodeAddress extracts address correctly") {
      val hex = "0x000000000000000000000000eb2cb5e155ac36c2d50be9750bfcab6fc1f3a607"
      val addr = AbiCodec.decodeAddress(hex)
      assertTrue(addr.toLowerCase == "0xeb2cb5e155ac36c2d50be9750bfcab6fc1f3a607")
    },
    test("FunctionSelectors lookup works for known selectors") {
      val result = FunctionSelectors.lookup("0x760d9bba")
      assertTrue(result.isDefined && result.get.lspContext == "LSP7 token transfer")
    },
    test("FunctionSelectors lookup returns None for unknown") {
      val result = FunctionSelectors.lookup("0xdeadbeef")
      assertTrue(result.isEmpty)
    }
  )
