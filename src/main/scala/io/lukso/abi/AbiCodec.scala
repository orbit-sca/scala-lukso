package io.lukso.abi

import org.web3j.abi.{FunctionEncoder, FunctionReturnDecoder, TypeReference}
import org.web3j.abi.datatypes.{Function as AbiFunction, Type, Bool, Address as AbiAddress, DynamicBytes, Utf8String}
import org.web3j.abi.datatypes.generated.{Bytes4, Bytes32, Uint256}

import java.math.BigInteger
import scala.jdk.CollectionConverters.*

object AbiCodec:

  /** Encode a function call to hex calldata (with 0x prefix). */
  def encodeFunctionCall(
    functionName: String,
    inputParams:  List[Type[?]],
    outputParams: List[TypeReference[?]]
  ): String =
    val function = new AbiFunction(functionName, inputParams.asJava, outputParams.asJava)
    val encoded = FunctionEncoder.encode(function)
    if encoded.startsWith("0x") then encoded else "0x" + encoded

  /** Decode return data from an eth_call. */
  @SuppressWarnings(Array("unchecked"))
  def decodeReturnValues(
    hexData:      String,
    outputParams: List[TypeReference[?]]
  ): List[Type[?]] =
    val javaList = outputParams.asInstanceOf[List[TypeReference[Type[?]]]].asJava
    FunctionReturnDecoder.decode(hexData, javaList).asScala.toList

  // ── Pre-built function signatures ──

  /** ERC165: supportsInterface(bytes4) returns (bool) */
  def encodeSupportsInterface(interfaceId: String): String =
    val clean = if interfaceId.startsWith("0x") then interfaceId.drop(2) else interfaceId
    val padded = (clean + "0" * (8 - clean.length)).take(8)
    val bytes4 = new Bytes4(hexToBytes(padded))
    encodeFunctionCall(
      "supportsInterface",
      List(bytes4),
      List(new TypeReference[Bool]() {})
    )

  /** ERC725Y: getData(bytes32) returns (bytes) */
  def encodeGetData(dataKey: String): String =
    val clean = if dataKey.startsWith("0x") then dataKey.drop(2) else dataKey
    val padded = clean.padTo(64, '0')
    val bytes32 = new Bytes32(hexToBytes(padded))
    encodeFunctionCall(
      "getData",
      List(bytes32),
      List(new TypeReference[DynamicBytes]() {})
    )

  /** ERC725Y: getDataBatch(bytes32[]) returns (bytes[]) */
  def encodeGetDataBatch(dataKeys: List[String]): String =
    val keysArray = new org.web3j.abi.datatypes.DynamicArray(
      classOf[Bytes32],
      dataKeys.map { k =>
        val clean = if k.startsWith("0x") then k.drop(2) else k
        new Bytes32(hexToBytes(clean.padTo(64, '0')))
      }.asJava
    )
    encodeFunctionCall(
      "getDataBatch",
      List(keysArray),
      List(new TypeReference[org.web3j.abi.datatypes.DynamicArray[DynamicBytes]]() {})
    )

  /** ERC1271: isValidSignature(bytes32, bytes) returns (bytes4) */
  def encodeIsValidSignature(hash: String, signature: String): String =
    val hashClean = if hash.startsWith("0x") then hash.drop(2) else hash
    val sigClean  = if signature.startsWith("0x") then signature.drop(2) else signature
    val hashBytes = new Bytes32(hexToBytes(hashClean.padTo(64, '0')))
    val sigBytes  = new DynamicBytes(hexToBytes(sigClean))
    encodeFunctionCall(
      "isValidSignature",
      List(hashBytes, sigBytes),
      List(new TypeReference[Bytes4]() {})
    )

  /** owner() returns (address) */
  def encodeOwner(): String =
    encodeFunctionCall("owner", List.empty, List(new TypeReference[AbiAddress]() {}))

  /** name() returns (string) */
  def encodeName(): String =
    encodeFunctionCall("name", List.empty, List(new TypeReference[Utf8String]() {}))

  /** symbol() returns (string) */
  def encodeSymbol(): String =
    encodeFunctionCall("symbol", List.empty, List(new TypeReference[Utf8String]() {}))

  /** totalSupply() returns (uint256) */
  def encodeTotalSupply(): String =
    encodeFunctionCall("totalSupply", List.empty, List(new TypeReference[Uint256]() {}))

  /** decimals() returns (uint256) */
  def encodeDecimals(): String =
    encodeFunctionCall("decimals", List.empty, List(new TypeReference[Uint256]() {}))

  /** balanceOf(address) returns (uint256) */
  def encodeBalanceOf(address: String): String =
    val addr = new AbiAddress(address)
    encodeFunctionCall("balanceOf", List(addr), List(new TypeReference[Uint256]() {}))

  /** tokenIdsOf(address) returns (bytes32[]) */
  def encodeTokenIdsOf(address: String): String =
    val addr = new AbiAddress(address)
    encodeFunctionCall(
      "tokenIdsOf",
      List(addr),
      List(new TypeReference[org.web3j.abi.datatypes.DynamicArray[Bytes32]]() {})
    )

  /** tokenOwnerOf(bytes32) returns (address) */
  def encodeTokenOwnerOf(tokenId: String): String =
    val clean = if tokenId.startsWith("0x") then tokenId.drop(2) else tokenId
    val id = new Bytes32(hexToBytes(clean.padTo(64, '0')))
    encodeFunctionCall(
      "tokenOwnerOf",
      List(id),
      List(new TypeReference[AbiAddress]() {})
    )

  // ── Decode helpers ──

  /** Decode a hex return value as a bool. */
  def decodeBool(hex: String): Boolean =
    val results = decodeReturnValues(hex, List(new TypeReference[Bool]() {}))
    results.headOption.exists(_.asInstanceOf[Bool].getValue)

  /** Decode a hex return value as a uint256. */
  def decodeUint256(hex: String): BigInteger =
    val results = decodeReturnValues(hex, List(new TypeReference[Uint256]() {}))
    results.headOption.map(_.asInstanceOf[Uint256].getValue).getOrElse(BigInteger.ZERO)

  /** Decode a hex return value as an address. */
  def decodeAddress(hex: String): String =
    val results = decodeReturnValues(hex, List(new TypeReference[AbiAddress]() {}))
    results.headOption.map(_.asInstanceOf[AbiAddress].getValue).getOrElse("")

  /** Decode a hex return value as a UTF-8 string. */
  def decodeString(hex: String): String =
    val results = decodeReturnValues(hex, List(new TypeReference[Utf8String]() {}))
    results.headOption.map(_.asInstanceOf[Utf8String].getValue).getOrElse("")

  /** Decode a hex return value as dynamic bytes. */
  def decodeBytes(hex: String): Array[Byte] =
    val results = decodeReturnValues(hex, List(new TypeReference[DynamicBytes]() {}))
    results.headOption.map(_.asInstanceOf[DynamicBytes].getValue).getOrElse(Array.emptyByteArray)

  /** Helper: convert hex string to byte array. */
  private[lukso] def hexToBytes(hex: String): Array[Byte] =
    val clean = if hex.startsWith("0x") then hex.drop(2) else hex
    if clean.isEmpty then Array.emptyByteArray
    else clean.grouped(2).map(Integer.parseInt(_, 16).toByte).toArray
