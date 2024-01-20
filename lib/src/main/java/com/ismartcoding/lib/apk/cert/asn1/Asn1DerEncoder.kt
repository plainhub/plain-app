package com.ismartcoding.lib.apk.cert.asn1

import com.ismartcoding.lib.apk.cert.asn1.ber.BerEncoding
import java.io.ByteArrayOutputStream
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.math.BigInteger
import java.nio.ByteBuffer
import java.util.Collections

/**
 * Encoder of ASN.1 structures into DER-encoded form.
 *
 *
 *
 * Structure is described to the encoder by providing a class annotated with [Asn1Class],
 * containing fields annotated with [Asn1Field].
 */
object Asn1DerEncoder {
    /**
     * Returns the DER-encoded form of the provided ASN.1 structure.
     *
     * @param container container to be encoded. The container's class must meet the following
     * requirements:
     *
     *  * The class must be annotated with [Asn1Class].
     *  * Member fields of the class which are to be encoded must be annotated with
     * [Asn1Field] and be public.
     *
     * @throws Asn1EncodingException if the input could not be encoded
     */
    @Throws(Asn1EncodingException::class)
    fun encode(container: Any): ByteArray? {
        val containerClass: Class<*> = container.javaClass
        val containerAnnotation = containerClass.getAnnotation(
            Asn1Class::class.java
        )
            ?: throw Asn1EncodingException(
                containerClass.name + " not annotated with " + Asn1Class::class.java.name
            )
        val containerType = containerAnnotation.type
        return when (containerType) {
            Asn1Type.CHOICE -> toChoice(container)
            Asn1Type.SEQUENCE -> toSequence(container)
            else -> throw Asn1EncodingException("Unsupported container type: $containerType")
        }
    }

    @Throws(Asn1EncodingException::class)
    private fun toChoice(container: Any): ByteArray? {
        val containerClass: Class<*> = container.javaClass
        val fields = getAnnotatedFields(container)
        if (fields.isEmpty()) {
            throw Asn1EncodingException(
                "No fields annotated with " + Asn1Field::class.java.name
                        + " in CHOICE class " + containerClass.name
            )
        }
        var resultField: AnnotatedField? = null
        for (field in fields) {
            val fieldValue = getMemberFieldValue(container, field.field)
            if (fieldValue != null) {
                if (resultField != null) {
                    throw Asn1EncodingException(
                        "Multiple non-null fields in CHOICE class " + containerClass.name
                                + ": " + resultField.field.getName()
                                + ", " + field.field.getName()
                    )
                }
                resultField = field
            }
        }
        if (resultField == null) {
            throw Asn1EncodingException(
                "No non-null fields in CHOICE class " + containerClass.name
            )
        }
        return resultField.toDer()
    }

    @Throws(Asn1EncodingException::class)
    private fun toSequence(container: Any): ByteArray {
        val containerClass: Class<*> = container.javaClass
        val fields = getAnnotatedFields(container)
        Collections.sort<AnnotatedField>(
            fields
        ) { f1, f2 -> f1.annotation.index - f2.annotation.index }
        if (fields.size > 1) {
            var lastField: AnnotatedField? = null
            for (field in fields) {
                if (lastField != null && lastField.annotation.index == field.annotation.index) {
                    throw Asn1EncodingException(
                        "Fields have the same index: " + containerClass.name
                                + "." + lastField.field.getName()
                                + " and ." + field.field.getName()
                    )
                }
                lastField = field
            }
        }
        val serializedFields: MutableList<ByteArray> = ArrayList(fields.size)
        for (field in fields) {
            var serializedField: ByteArray?
            serializedField = try {
                field.toDer()
            } catch (e: Asn1EncodingException) {
                throw Asn1EncodingException(
                    "Failed to encode " + containerClass.name
                            + "." + field.field.getName(),
                    e
                )
            }
            if (serializedField != null) {
                serializedFields.add(serializedField)
            }
        }
        return createTag(
            BerEncoding.TAG_CLASS_UNIVERSAL, true, BerEncoding.TAG_NUMBER_SEQUENCE,
            *serializedFields.toTypedArray()
        )
    }

    @Throws(Asn1EncodingException::class)
    private fun toSetOf(values: Collection<*>, elementType: Asn1Type?): ByteArray {
        val serializedValues: MutableList<ByteArray> = ArrayList(values.size)
        for (value in values) {
            value?.let { JavaToDerConverter.toDer(it, elementType, null) }
                ?.let { serializedValues.add(it) }
        }
        if (serializedValues.size > 1) {
            Collections.sort(serializedValues, ByteArrayLexicographicComparator.INSTANCE)
        }
        return createTag(
            BerEncoding.TAG_CLASS_UNIVERSAL, true, BerEncoding.TAG_NUMBER_SET, *serializedValues.toTypedArray()
        )
    }

    @Throws(Asn1EncodingException::class)
    private fun getAnnotatedFields(container: Any): List<AnnotatedField> {
        val containerClass: Class<*> = container.javaClass
        val declaredFields = containerClass.declaredFields
        val result: MutableList<AnnotatedField> = ArrayList(declaredFields.size)
        for (field in declaredFields) {
            val annotation = field.getAnnotation(
                Asn1Field::class.java
            ) ?: continue
            if (Modifier.isStatic(field.modifiers)) {
                throw Asn1EncodingException(
                    Asn1Field::class.java.name + " used on a static field: "
                            + containerClass.name + "." + field.name
                )
            }
            var annotatedField: AnnotatedField
            annotatedField = try {
                AnnotatedField(container, field, annotation)
            } catch (e: Asn1EncodingException) {
                throw Asn1EncodingException(
                    "Invalid ASN.1 annotation on "
                            + containerClass.name + "." + field.name,
                    e
                )
            }
            result.add(annotatedField)
        }
        return result
    }

    private fun toInteger(value: Int): ByteArray {
        return toInteger(value.toLong())
    }

    private fun toInteger(value: Long): ByteArray {
        return toInteger(BigInteger.valueOf(value))
    }

    private fun toInteger(value: BigInteger): ByteArray {
        return createTag(
            BerEncoding.TAG_CLASS_UNIVERSAL, false, BerEncoding.TAG_NUMBER_INTEGER,
            value.toByteArray()
        )
    }

    @Throws(Asn1EncodingException::class)
    private fun toOid(oid: String): ByteArray {
        val encodedValue = ByteArrayOutputStream()
        val nodes = oid.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        if (nodes.size < 2) {
            throw Asn1EncodingException(
                "OBJECT IDENTIFIER must contain at least two nodes: $oid"
            )
        }
        val firstNode: Int
        firstNode = try {
            nodes[0].toInt()
        } catch (e: NumberFormatException) {
            throw Asn1EncodingException("Node #1 not numeric: " + nodes[0])
        }
        if (firstNode > 6 || firstNode < 0) {
            throw Asn1EncodingException("Invalid value for node #1: $firstNode")
        }
        val secondNode: Int
        secondNode = try {
            nodes[1].toInt()
        } catch (e: NumberFormatException) {
            throw Asn1EncodingException("Node #2 not numeric: " + nodes[1])
        }
        if (secondNode >= 40 || secondNode < 0) {
            throw Asn1EncodingException("Invalid value for node #2: $secondNode")
        }
        val firstByte = firstNode * 40 + secondNode
        if (firstByte > 0xff) {
            throw Asn1EncodingException(
                "First two nodes out of range: $firstNode.$secondNode"
            )
        }
        encodedValue.write(firstByte)
        for (i in 2 until nodes.size) {
            val nodeString = nodes[i]
            var node: Int
            node = try {
                nodeString.toInt()
            } catch (e: NumberFormatException) {
                throw Asn1EncodingException("Node #" + (i + 1) + " not numeric: " + nodeString)
            }
            if (node < 0) {
                throw Asn1EncodingException("Invalid value for node #" + (i + 1) + ": " + node)
            }
            if (node <= 0x7f) {
                encodedValue.write(node)
                continue
            }
            if (node < 1 shl 14) {
                encodedValue.write(0x80 or (node shr 7))
                encodedValue.write(node and 0x7f)
                continue
            }
            if (node < 1 shl 21) {
                encodedValue.write(0x80 or (node shr 14))
                encodedValue.write(0x80 or (node shr 7 and 0x7f))
                encodedValue.write(node and 0x7f)
                continue
            }
            throw Asn1EncodingException("Node #" + (i + 1) + " too large: " + node)
        }
        return createTag(
            BerEncoding.TAG_CLASS_UNIVERSAL, false, BerEncoding.TAG_NUMBER_OBJECT_IDENTIFIER,
            encodedValue.toByteArray()
        )
    }

    @Throws(Asn1EncodingException::class)
    private fun getMemberFieldValue(obj: Any, field: Field): Any {
        return try {
            field[obj]
        } catch (e: ReflectiveOperationException) {
            throw Asn1EncodingException(
                "Failed to read " + obj.javaClass.name + "." + field.name, e
            )
        }
    }

    private fun createTag(
        tagClass: Int, constructed: Boolean, tagNumber: Int, vararg contents: ByteArray?
    ): ByteArray {
        require(tagNumber < 0x1f) { "High tag numbers not supported: $tagNumber" }
        // tag class & number fit into the first byte
        val firstIdentifierByte =
            (tagClass shl 6 or (if (constructed) 1 shl 5 else 0) or tagNumber).toByte()
        var contentsLength = 0
        for (c in contents) {
            contentsLength += c!!.size
        }
        var contentsPosInResult: Int
        val result: ByteArray
        if (contentsLength < 0x80) {
            // Length fits into one byte
            contentsPosInResult = 2
            result = ByteArray(contentsPosInResult + contentsLength)
            result[0] = firstIdentifierByte
            result[1] = contentsLength.toByte()
        } else {
            // Length is represented as multiple bytes
            // The low 7 bits of the first byte represent the number of length bytes (following the
            // first byte) in which the length is in big-endian base-256 form
            if (contentsLength <= 0xff) {
                contentsPosInResult = 3
                result = ByteArray(contentsPosInResult + contentsLength)
                result[1] = 0x81.toByte() // 1 length byte
                result[2] = contentsLength.toByte()
            } else if (contentsLength <= 0xffff) {
                contentsPosInResult = 4
                result = ByteArray(contentsPosInResult + contentsLength)
                result[1] = 0x82.toByte() // 2 length bytes
                result[2] = (contentsLength shr 8).toByte()
                result[3] = (contentsLength and 0xff).toByte()
            } else if (contentsLength <= 0xffffff) {
                contentsPosInResult = 5
                result = ByteArray(contentsPosInResult + contentsLength)
                result[1] = 0x83.toByte() // 3 length bytes
                result[2] = (contentsLength shr 16).toByte()
                result[3] = (contentsLength shr 8 and 0xff).toByte()
                result[4] = (contentsLength and 0xff).toByte()
            } else {
                contentsPosInResult = 6
                result = ByteArray(contentsPosInResult + contentsLength)
                result[1] = 0x84.toByte() // 4 length bytes
                result[2] = (contentsLength shr 24).toByte()
                result[3] = (contentsLength shr 16 and 0xff).toByte()
                result[4] = (contentsLength shr 8 and 0xff).toByte()
                result[5] = (contentsLength and 0xff).toByte()
            }
            result[0] = firstIdentifierByte
        }
        for (c in contents) {
            System.arraycopy(c!!, 0, result, contentsPosInResult, c.size)
            contentsPosInResult += c.size
        }
        return result
    }

    /**
     * Compares two bytes arrays based on their lexicographic order. Corresponding elements of the
     * two arrays are compared in ascending order. Elements at out of range indices are assumed to
     * be smaller than the smallest possible value for an element.
     */
    private class ByteArrayLexicographicComparator : Comparator<ByteArray> {
        override fun compare(arr1: ByteArray, arr2: ByteArray): Int {
            val commonLength = Math.min(arr1.size, arr2.size)
            for (i in 0 until commonLength) {
                val diff = (arr1[i].toInt() and 0xff) - (arr2[i].toInt() and 0xff)
                if (diff != 0) {
                    return diff
                }
            }
            return arr1.size - arr2.size
        }

        companion object {
            val INSTANCE = ByteArrayLexicographicComparator()
        }
    }

    private class AnnotatedField(
        private val mObject: Any,
        val field: Field,
        val annotation: Asn1Field
    ) {
        private val mDataType: Asn1Type = annotation.type
        private val mElementDataType: Asn1Type = annotation.elementType
        private val mTagClass: Asn1TagClass
        private val mDerTagClass: Int
        private val mDerTagNumber: Int
        private val mTagging: Asn1Tagging
        private val mOptional: Boolean

        init {
            var tagClass = annotation.cls
            if (tagClass == Asn1TagClass.AUTOMATIC) {
                tagClass = if (annotation.tagNumber != -1) {
                    Asn1TagClass.CONTEXT_SPECIFIC
                } else {
                    Asn1TagClass.UNIVERSAL
                }
            }
            mTagClass = tagClass
            mDerTagClass = BerEncoding.getTagClass(mTagClass)
            val tagNumber: Int
            tagNumber = if (annotation.tagNumber != -1) {
                annotation.tagNumber
            } else if (mDataType == Asn1Type.CHOICE || mDataType == Asn1Type.ANY) {
                -1
            } else {
                BerEncoding.getTagNumber(mDataType)
            }
            mDerTagNumber = tagNumber
            mTagging = annotation.tagging
            if ((mTagging == Asn1Tagging.EXPLICIT || mTagging == Asn1Tagging.IMPLICIT) && annotation.tagNumber == -1) {
                throw Asn1EncodingException(
                    "Tag number must be specified when tagging mode is $mTagging"
                )
            }
            mOptional = annotation.optional
        }

        @Throws(Asn1EncodingException::class)
        fun toDer(): ByteArray? {
            val fieldValue = getMemberFieldValue(mObject, field)
            if (fieldValue == null) {
                if (mOptional) {
                    return null
                }
                throw Asn1EncodingException("Required field not set")
            }
            val encoded = JavaToDerConverter.toDer(fieldValue, mDataType, mElementDataType)
            return when (mTagging) {
                Asn1Tagging.NORMAL -> encoded
                Asn1Tagging.EXPLICIT -> encoded?.let {
                    createTag(mDerTagClass, true, mDerTagNumber,
                        it
                    )
                }
                Asn1Tagging.IMPLICIT -> {
                    val originalTagNumber = BerEncoding.getTagNumber(
                        encoded!![0]
                    )
                    if (originalTagNumber == 0x1f) {
                        throw Asn1EncodingException("High-tag-number form not supported")
                    }
                    if (mDerTagNumber >= 0x1f) {
                        throw Asn1EncodingException(
                            "Unsupported high tag number: $mDerTagNumber"
                        )
                    }
                    encoded[0] = BerEncoding.setTagNumber(
                        encoded[0], mDerTagNumber
                    )
                    encoded[0] = BerEncoding.setTagClass(encoded[0], mDerTagClass)
                    encoded
                }

                else -> throw RuntimeException("Unknown tagging mode: $mTagging")
            }
        }
    }

    private object JavaToDerConverter {
        @Throws(Asn1EncodingException::class)
        fun toDer(source: Any, targetType: Asn1Type?, targetElementType: Asn1Type?): ByteArray? {
            val sourceType: Class<*> = source.javaClass
            if (Asn1OpaqueObject::class.java == sourceType) {
                val buf = (source as Asn1OpaqueObject).encoded
                val result = ByteArray(buf.remaining())
                buf[result]
                return result
            }
            if (targetType == null || targetType == Asn1Type.ANY) {
                return encode(source)
            }
            when (targetType) {
                Asn1Type.OCTET_STRING -> {
                    var value: ByteArray? = null
                    if (source is ByteBuffer) {
                        val buf = source
                        value = ByteArray(buf.remaining())
                        buf.slice()[value]
                    } else if (source is ByteArray) {
                        value = source
                    }
                    if (value != null) {
                        return createTag(
                            BerEncoding.TAG_CLASS_UNIVERSAL,
                            false,
                            BerEncoding.TAG_NUMBER_OCTET_STRING,
                            value
                        )
                    }
                }

                Asn1Type.INTEGER -> if (source is Int) {
                    return toInteger(source)
                } else if (source is Long) {
                    return toInteger(source)
                } else if (source is BigInteger) {
                    return toInteger(source)
                }

                Asn1Type.OBJECT_IDENTIFIER -> if (source is String) {
                    return toOid(source)
                }

                Asn1Type.SEQUENCE -> {
                    val containerAnnotation = sourceType.getAnnotation(
                        Asn1Class::class.java
                    )
                    if (containerAnnotation != null && containerAnnotation.type == Asn1Type.SEQUENCE) {
                        return toSequence(source)
                    }
                }

                Asn1Type.CHOICE -> {
                    val containerAnnotation = sourceType.getAnnotation(
                        Asn1Class::class.java
                    )
                    if (containerAnnotation != null && containerAnnotation.type == Asn1Type.CHOICE) {
                        return toChoice(source)
                    }
                }

                Asn1Type.SET_OF -> return toSetOf(source as Collection<*>, targetElementType)
                else -> {}
            }
            throw Asn1EncodingException(
                "Unsupported conversion: " + sourceType.name + " to ASN.1 " + targetType
            )
        }
    }
}