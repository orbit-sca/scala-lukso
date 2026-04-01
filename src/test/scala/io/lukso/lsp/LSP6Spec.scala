package io.lukso.lsp

import io.lukso.models.Permission
import zio.test.*

object LSP6Spec extends ZIOSpecDefault:
  def spec = suite("LSP6 Permission Decoding")(
    test("decode Mustard's permission bitmask 0x632E00") {
      // Mustard has: CALL, DEPLOY, TRANSFERVALUE, SUPER_CALL, STATICCALL, SUPER_SETDATA, SIGN, EXECUTE_RELAY_CALL
      val raw = BigInt("632E00", 16)
      val flags = Permission.decode(raw)
      assertTrue(
        flags("CALL") == true,
        flags("DEPLOY") == true,
        flags("TRANSFERVALUE") == true,
        flags("SUPER_CALL") == true,
        flags("STATICCALL") == true,
        flags("SUPER_SETDATA") == true,
        flags("SIGN") == true,
        flags("EXECUTE_RELAY_CALL") == true,
        flags("CHANGEOWNER") == false,
        flags("SETDATA") == false,
        flags("ADDCONTROLLER") == false
      )
    },
    test("decode zero permissions") {
      val flags = Permission.decode(BigInt(0))
      assertTrue(flags.values.forall(_ == false))
    },
    test("decode all permissions (0x7FFFFF)") {
      val flags = Permission.decode(BigInt("7FFFFF", 16))
      assertTrue(flags.values.forall(_ == true))
    },
    test("decode single SIGN permission") {
      val flags = Permission.decode(BigInt("200000", 16))
      assertTrue(
        flags("SIGN") == true,
        flags.count(_._2) == 1
      )
    },
    test("permission key construction") {
      val key = DataKeys.permissionKeyFor("0xA56768F0f7FA8906a569565dDCD479c8905e4CB2")
      assertTrue(key == "0x4b80742de2bf82acb3630000a56768f0f7fa8906a569565ddcd479c8905e4cb2")
    }
  )
