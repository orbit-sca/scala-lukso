package io.lukso.crypto

import org.bouncycastle.jcajce.provider.digest.Keccak

object Keccak256:

  /** Compute keccak256 hash of raw bytes. Returns 32 bytes. */
  def hash(input: Array[Byte]): Array[Byte] =
    val digest = new Keccak.Digest256()
    digest.update(input)
    digest.digest()

  /** Compute keccak256 of a UTF-8 string. Returns hex with 0x prefix. */
  def hashString(input: String): String =
    val bytes = hash(input.getBytes("UTF-8"))
    "0x" + bytes.map("%02x".format(_)).mkString

  /** Compute keccak256 of hex data (with or without 0x prefix). Returns hex with 0x prefix. */
  def hashHex(hexInput: String): String =
    val clean = if hexInput.startsWith("0x") then hexInput.drop(2) else hexInput
    val bytes = clean.grouped(2).map(Integer.parseInt(_, 16).toByte).toArray
    val result = hash(bytes)
    "0x" + result.map("%02x".format(_)).mkString

  /** ERC725Y singleton data key: keccak256(keyName). */
  def dataKey(keyName: String): String = hashString(keyName)

  /** LSP6 MappingWithGrouping key: prefix + address (20 bytes, lowercase). */
  def permissionKey(prefix: String, address: String): String =
    val addr = if address.startsWith("0x") then address.drop(2).toLowerCase else address.toLowerCase
    prefix + addr

  /** Convert hex string to byte array. */
  def hexToBytes(hex: String): Array[Byte] =
    val clean = if hex.startsWith("0x") then hex.drop(2) else hex
    clean.grouped(2).map(Integer.parseInt(_, 16).toByte).toArray

  /** Convert byte array to hex string with 0x prefix. */
  def bytesToHex(bytes: Array[Byte]): String =
    "0x" + bytes.map("%02x".format(_)).mkString
