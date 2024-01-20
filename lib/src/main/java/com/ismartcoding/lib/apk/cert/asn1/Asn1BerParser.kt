package com.ismartcoding.lib.apk.cert.asn1

import com.ismartcoding.lib.apk.cert.asn1.ber.BerDataValue
import com.ismartcoding.lib.apk.cert.asn1.ber.BerDataValueFormatException
import com.ismartcoding.lib.apk.cert.asn1.ber.BerEncoding
import com.ismartcoding.lib.apk.cert.asn1.ber.ByteBufferBerDataValueReader
import com.ismartcoding.lib.apk.utils.Buffers.readBytes
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.math.BigInteger
import java.nio.ByteBuffer
import java.util.Collections

/**
 * Parser of ASN.1 BER-encoded structures.
 *
 *
 *
 * Structure is described to the parser by providing a class annotated with [Asn1Class],
 * containing fields annotated with [Asn1Field].
 */
object Asn1BerParser {
    /**
     * Returns the ASN.1 structure contained in the BER encoded input.
     *
     * @param encoded        encoded input. If the decoding operation succeeds, the position of this buffer
     * is advanced to the first position following the end of the consumed structure.
     * @param containerClass class describing the structure of the input. The class must meet the
     * following requirements:
     *
     *  * The class must be annotated with [Asn1Class].
     *  * The class must expose a public no-arg constructor.
     *  * Member fields of the class which are populated with parsed input must be
     * annotated with [Asn1Field] and be public and non-final.
     *
     * @throws Asn1DecodingException if the input could not be decoded into the specified Java
     * object
     */
    @Throws(Asn1DecodingException::class)
    fun <T: Any> parse(encoded: ByteBuffer?, containerClass: Class<T>?): T {
        val containerDataValue: BerDataValue = try {
            ByteBufferBerDataValueReader(encoded).readDataValue()
        } catch (e: BerDataValueFormatException) {
            throw Asn1DecodingException("Failed to decode top-level data value", e)
        } ?: throw Asn1DecodingException("Empty input")
        return parse(containerDataValue, containerClass)
    }

    /**
     * Returns the implicit `SET OF` contained in the provided ASN.1 BER input. Implicit means
     * that this method does not care whether the tag number of this data structure is
     * `SET OF` and whether the tag class is `UNIVERSAL`.
     *
     *
     *
     * Note: The returned type is [List] rather than [java.util.Set] because ASN.1
     * SET may contain duplicate elements.
     *
     * @param encoded      encoded input. If the decoding operation succeeds, the position of this buffer
     * is advanced to the first position following the end of the consumed structure.
     * @param elementClass class describing the structure of the values/elements contained in this
     * container. The class must meet the following requirements:
     *
     *  * The class must be annotated with [Asn1Class].
     *  * The class must expose a public no-arg constructor.
     *  * Member fields of the class which are populated with parsed input must be
     * annotated with [Asn1Field] and be public and non-final.
     *
     * @throws Asn1DecodingException if the input could not be decoded into the specified Java
     * object
     */
    @Throws(Asn1DecodingException::class)
    fun <T : Any> parseImplicitSetOf(encoded: ByteBuffer?, elementClass: Class<T>): List<T?> {
        val containerDataValue: BerDataValue = try {
            ByteBufferBerDataValueReader(encoded).readDataValue()
        } catch (e: BerDataValueFormatException) {
            throw Asn1DecodingException("Failed to decode top-level data value", e)
        } ?: throw Asn1DecodingException("Empty input")
        return parseSetOf(containerDataValue, elementClass)
    }

    @Throws(Asn1DecodingException::class)
    private fun <T : Any> parse(container: BerDataValue?, containerClass: Class<T>?): T {
        if (container == null) {
            throw NullPointerException("container == null")
        }
        if (containerClass == null) {
            throw NullPointerException("containerClass == null")
        }
        val dataType = getContainerAsn1Type(containerClass)
        return when (dataType) {
            Asn1Type.CHOICE -> parseChoice(container, containerClass)
            Asn1Type.SEQUENCE -> {
                val expectedTagClass = BerEncoding.TAG_CLASS_UNIVERSAL
                val expectedTagNumber = BerEncoding.getTagNumber(dataType)
                if (container.tagClass != expectedTagClass || container.tagNumber != expectedTagNumber) {
                    throw Asn1UnexpectedTagException(
                        "Unexpected data value read as " + containerClass.name
                                + ". Expected " + BerEncoding.tagClassAndNumberToString(
                            expectedTagClass, expectedTagNumber
                        )
                                + ", but read: " + BerEncoding.tagClassAndNumberToString(
                            container.tagClass, container.tagNumber
                        )
                    )
                }
                parseSequence(container, containerClass)
            }

            else -> throw Asn1DecodingException("Parsing container $dataType not supported")
        }
    }

    @Throws(Asn1DecodingException::class)
    private fun <T : Any> parseChoice(dataValue: BerDataValue?, containerClass: Class<T>): T {
        val fields = getAnnotatedFields(containerClass)
        if (fields.isEmpty()) {
            throw Asn1DecodingException(
                "No fields annotated with " + Asn1Field::class.java.name
                        + " in CHOICE class " + containerClass.name
            )
        }

        // Check that class + tagNumber don't clash between the choices
        for (i in 0 until fields.size - 1) {
            val f1 = fields[i]
            val tagNumber1: Int = f1.berTagNumber
            val tagClass1: Int = f1.berTagClass
            for (j in i + 1 until fields.size) {
                val f2 = fields[j]
                val tagNumber2: Int = f2.berTagNumber
                val tagClass2: Int = f2.berTagClass
                if (tagNumber1 == tagNumber2 && tagClass1 == tagClass2) {
                    throw Asn1DecodingException(
                        "CHOICE fields are indistinguishable because they have the same tag"
                                + " class and number: " + containerClass.name
                                + "." + f1.field.getName()
                                + " and ." + f2.field.getName()
                    )
                }
            }
        }

        // Instantiate the container object / result
        val obj: T = try {
            containerClass.getConstructor().newInstance()
        } catch (e: IllegalArgumentException) {
            throw Asn1DecodingException("Failed to instantiate " + containerClass.name, e)
        } catch (e: ReflectiveOperationException) {
            throw Asn1DecodingException("Failed to instantiate " + containerClass.name, e)
        }

        // Set the matching field's value from the data value
        for (field in fields) {
            try {
                field.setValueFrom(dataValue, obj)
                return obj
            } catch (expected: Asn1UnexpectedTagException) {
                // not a match
            }
        }
        throw Asn1DecodingException(
            "No options of CHOICE " + containerClass.name + " matched"
        )
    }

    @Throws(Asn1DecodingException::class)
    private fun <T : Any> parseSequence(container: BerDataValue?, containerClass: Class<T>): T {
        val fields = getAnnotatedFields(containerClass)
        Collections.sort<AnnotatedField>(
            fields
        ) { f1, f2 -> f1.annotation.index - f2.annotation.index }
        // Check that there are no fields with the same index
        if (fields.size > 1) {
            var lastField: AnnotatedField? = null
            for (field in fields) {
                if (lastField != null && lastField.annotation.index == field.annotation.index) {
                    throw Asn1DecodingException(
                        "Fields have the same index: " + containerClass.name
                                + "." + lastField.field.getName()
                                + " and ." + field.field.getName()
                    )
                }
                lastField = field
            }
        }

        // Instantiate the container object / result
        val t: T = try {
            containerClass.getConstructor().newInstance()
        } catch (e: IllegalArgumentException) {
            throw Asn1DecodingException("Failed to instantiate " + containerClass.name, e)
        } catch (e: ReflectiveOperationException) {
            throw Asn1DecodingException("Failed to instantiate " + containerClass.name, e)
        }

        // Parse fields one by one. A complication is that there may be optional fields.
        var nextUnreadFieldIndex = 0
        val elementsReader = container!!.contentsReader()
        while (nextUnreadFieldIndex < fields.size) {
            var dataValue: BerDataValue?
            dataValue = try {
                elementsReader.readDataValue()
            } catch (e: BerDataValueFormatException) {
                throw Asn1DecodingException("Malformed data value", e)
            }
            if (dataValue == null) {
                break
            }
            for (i in nextUnreadFieldIndex until fields.size) {
                val field = fields[i]
                try {
                    if (field.isOptional) {
                        // Optional field -- might not be present and we may thus be trying to set
                        // it from the wrong tag.
                        try {
                            field.setValueFrom(dataValue, t)
                            nextUnreadFieldIndex = i + 1
                            break
                        } catch (e: Asn1UnexpectedTagException) {
                            // This field is not present, attempt to use this data value for the
                            // next / iteration of the loop
                            continue
                        }
                    } else {
                        // Mandatory field -- if we can't set its value from this data value, then
                        // it's an error
                        field.setValueFrom(dataValue, t)
                        nextUnreadFieldIndex = i + 1
                        break
                    }
                } catch (e: Asn1DecodingException) {
                    throw Asn1DecodingException(
                        "Failed to parse " + containerClass.name
                                + "." + field.field.getName(), e
                    )
                }
            }
        }
        return t
    }

    // NOTE: This method returns List rather than Set because ASN.1 SET_OF does require uniqueness
    // of elements -- it's an unordered collection.
    @Throws(Asn1DecodingException::class)
    private fun <T : Any> parseSetOf(container: BerDataValue?, elementClass: Class<T>): List<T?> {
        val result: MutableList<T?> = ArrayList()
        val elementsReader = container!!.contentsReader()
        while (true) {
            val dataValue = try {
                elementsReader.readDataValue()
            } catch (e: BerDataValueFormatException) {
                throw Asn1DecodingException("Malformed data value", e)
            } ?: break
            val element: T? = if (ByteBuffer::class.java == elementClass) {
                dataValue.encodedContents as T
            } else if (Asn1OpaqueObject::class.java == elementClass) {
                Asn1OpaqueObject(dataValue.encoded) as T
            } else {
                parse(dataValue, elementClass)
            }
            result.add(element)
        }
        return result
    }

    @Throws(Asn1DecodingException::class)
    private fun getContainerAsn1Type(containerClass: Class<*>): Asn1Type {
        val containerAnnotation = containerClass.getAnnotation(
            Asn1Class::class.java
        )
            ?: throw Asn1DecodingException(
                containerClass.name + " is not annotated with "
                        + Asn1Class::class.java.name
            )
        return when (containerAnnotation.type) {
            Asn1Type.CHOICE, Asn1Type.SEQUENCE -> containerAnnotation.type
            else -> throw Asn1DecodingException(
                "Unsupported ASN.1 container annotation type: "
                        + containerAnnotation.type
            )
        }
    }

    @Throws(Asn1DecodingException::class, ClassNotFoundException::class)
    private fun getElementType(field: Field): Class<*> {
        val type = field.genericType.toString()
        val delimiterIndex = type.indexOf('<')
        if (delimiterIndex == -1) {
            throw Asn1DecodingException("Not a container type: " + field.genericType)
        }
        val startIndex = delimiterIndex + 1
        val endIndex = type.indexOf('>', startIndex)
        // TODO: handle comma?
        if (endIndex == -1) {
            throw Asn1DecodingException("Not a container type: " + field.genericType)
        }
        val elementClassName = type.substring(startIndex, endIndex)
        return Class.forName(elementClassName)
    }

    @Throws(Asn1DecodingException::class)
    private fun oidToString(encodedOid: ByteBuffer?): String {
        if (!encodedOid!!.hasRemaining()) {
            throw Asn1DecodingException("Empty OBJECT IDENTIFIER")
        }

        // First component encodes the first two nodes, X.Y, as X * 40 + Y, with 0 <= X <= 2
        val firstComponent = decodeBase128UnsignedLong(encodedOid)
        val firstNode = Math.min(firstComponent / 40, 2).toInt()
        val secondNode = firstComponent - firstNode * 40L
        val result = StringBuilder()
        result.append(java.lang.Long.toString(firstNode.toLong())).append('.')
            .append(java.lang.Long.toString(secondNode))

        // Each consecutive node is encoded as a separate component
        while (encodedOid.hasRemaining()) {
            val node = decodeBase128UnsignedLong(encodedOid)
            result.append('.').append(java.lang.Long.toString(node))
        }
        return result.toString()
    }

    @Throws(Asn1DecodingException::class)
    private fun decodeBase128UnsignedLong(encoded: ByteBuffer?): Long {
        if (!encoded!!.hasRemaining()) {
            return 0
        }
        var result: Long = 0
        while (encoded.hasRemaining()) {
            if (result > Long.MAX_VALUE ushr 7) {
                throw Asn1DecodingException("Base-128 number too large")
            }
            val b = encoded.get().toInt() and 0xff
            result = result shl 7
            result = result or (b and 0x7f).toLong()
            if (b and 0x80 == 0) {
                return result
            }
        }
        throw Asn1DecodingException(
            "Truncated base-128 encoded input: missing terminating byte, with highest bit not"
                    + " set"
        )
    }

    private fun integerToBigInteger(encoded: ByteBuffer?): BigInteger {
        return if (!encoded!!.hasRemaining()) {
            BigInteger.ZERO
        } else BigInteger(readBytes(encoded))
    }

    @Throws(Asn1DecodingException::class)
    private fun integerToInt(encoded: ByteBuffer?): Int {
        val value = integerToBigInteger(encoded)
        return try {
            value.toInt()
        } catch (e: ArithmeticException) {
            throw Asn1DecodingException(
                String.format(
                    "INTEGER cannot be represented as int: %1\$d (0x%1\$x)",
                    value
                ), e
            )
        }
    }

    @Throws(Asn1DecodingException::class)
    private fun integerToLong(encoded: ByteBuffer?): Long {
        val value = integerToBigInteger(encoded)
        return try {
            value.toInt().toLong()
        } catch (e: ArithmeticException) {
            throw Asn1DecodingException(
                String.format("INTEGER cannot be represented as long: %1\$d (0x%1\$x)", value),
                e
            )
        }
    }

    @Throws(Asn1DecodingException::class)
    private fun getAnnotatedFields(containerClass: Class<*>): List<AnnotatedField> {
        val declaredFields = containerClass.declaredFields
        val result: MutableList<AnnotatedField> = ArrayList(declaredFields.size)
        for (field in declaredFields) {
            val annotation = field.getAnnotation(
                Asn1Field::class.java
            ) ?: continue
            if (Modifier.isStatic(field.modifiers)) {
                throw Asn1DecodingException(
                    Asn1Field::class.java.name + " used on a static field: "
                            + containerClass.name + "." + field.name
                )
            }
            var annotatedField: AnnotatedField
            annotatedField = try {
                AnnotatedField(field, annotation)
            } catch (e: Asn1DecodingException) {
                throw Asn1DecodingException(
                    "Invalid ASN.1 annotation on "
                            + containerClass.name + "." + field.name,
                    e
                )
            }
            result.add(annotatedField)
        }
        return result
    }

    private class AnnotatedField(val field: Field, val annotation: Asn1Field) {
        private val mDataType: Asn1Type = annotation.type
        private val mTagClass: Asn1TagClass
        val berTagClass: Int
        val berTagNumber: Int
        private val mTagging: Asn1Tagging
        val isOptional: Boolean

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
            berTagClass = BerEncoding.getTagClass(mTagClass)
            val tagNumber: Int
            tagNumber = if (annotation.tagNumber != -1) {
                annotation.tagNumber
            } else if (mDataType == Asn1Type.CHOICE || mDataType == Asn1Type.ANY) {
                -1
            } else {
                BerEncoding.getTagNumber(mDataType)
            }
            berTagNumber = tagNumber
            mTagging = annotation.tagging
            if ((mTagging == Asn1Tagging.EXPLICIT || mTagging == Asn1Tagging.IMPLICIT) && annotation.tagNumber == -1) {
                throw Asn1DecodingException(
                    "Tag number must be specified when tagging mode is $mTagging"
                )
            }
            isOptional = annotation.optional
        }

        @Throws(Asn1DecodingException::class)
        fun setValueFrom(dataValue: BerDataValue?, obj: Any) {
            var dataValue = dataValue
            val readTagClass = dataValue?.tagClass
            if (berTagNumber != -1) {
                val readTagNumber = dataValue?.tagNumber
                if (readTagClass != berTagClass || readTagNumber != berTagNumber) {
                    throw Asn1UnexpectedTagException(
                        "Tag mismatch. Expected: "
                                + BerEncoding.tagClassAndNumberToString(
                            berTagClass, berTagNumber
                        ) + ", but found "
                                + readTagClass?.let {
                            if (readTagNumber != null) {
                                BerEncoding.tagClassAndNumberToString(it, readTagNumber)
                            }
                        }
                    )
                }
            } else {
                if (readTagClass != berTagClass) {
                    throw Asn1UnexpectedTagException(
                        "Tag mismatch. Expected class: "
                                + BerEncoding.tagClassToString(berTagClass)
                                + ", but found "
                                + readTagClass?.let { BerEncoding.tagClassToString(it) }
                    )
                }
            }
            if (mTagging == Asn1Tagging.EXPLICIT) {
                dataValue = try {
                    dataValue!!.contentsReader().readDataValue()
                } catch (e: BerDataValueFormatException) {
                    throw Asn1DecodingException(
                        "Failed to read contents of EXPLICIT data value", e
                    )
                }
            }
            BerToJavaConverter.setFieldValue(obj, field, mDataType, dataValue)
        }
    }

    private class Asn1UnexpectedTagException(message: String?) : Asn1DecodingException(message) {
        companion object {
            private const val serialVersionUID = 1L
        }
    }

    private object BerToJavaConverter {
        @Throws(Asn1DecodingException::class)
        fun setFieldValue(
            obj: Any, field: Field, type: Asn1Type, dataValue: BerDataValue?
        ) {
            try {
                when (type) {
                    Asn1Type.SET_OF, Asn1Type.SEQUENCE_OF -> {
                        if (Asn1OpaqueObject::class.java == field.type) {
                            field[obj] = convert(type, dataValue, field.type)
                        } else {
                            field[obj] = parseSetOf(dataValue, getElementType(field))
                        }
                        return
                    }

                    else -> field[obj] = convert(type, dataValue, field.type)
                }
            } catch (e: ReflectiveOperationException) {
                throw Asn1DecodingException(
                    "Failed to set value of " + obj.javaClass.name
                            + "." + field.name,
                    e
                )
            }
        }

        private val EMPTY_BYTE_ARRAY = ByteArray(0)

        @Throws(Asn1DecodingException::class)
        fun <T : Any> convert(
            sourceType: Asn1Type,
            dataValue: BerDataValue?,
            targetType: Class<T>
        ): T? {
            if (ByteBuffer::class.java == targetType) {
                if (dataValue != null) {
                    return dataValue.encodedContents as T
                }
            } else if (ByteArray::class.java == targetType) {
                val resultBuf = dataValue?.encodedContents
                if (!resultBuf!!.hasRemaining()) {
                    return EMPTY_BYTE_ARRAY as T
                }
                val result = ByteArray(resultBuf.remaining())
                resultBuf[result]
                return result as T
            } else if (Asn1OpaqueObject::class.java == targetType) {
                if (dataValue != null) {
                    return Asn1OpaqueObject(dataValue.encoded) as T
                }
            }
            val encodedContents = dataValue?.encodedContents
            when (sourceType) {
                Asn1Type.INTEGER -> if (Int::class.javaPrimitiveType == targetType || Int::class.java == targetType) {
                    return Integer.valueOf(integerToInt(encodedContents)) as T
                } else if (Long::class.javaPrimitiveType == targetType || Long::class.java == targetType) {
                    return java.lang.Long.valueOf(integerToLong(encodedContents)) as T
                } else if (BigInteger::class.java == targetType) {
                    return integerToBigInteger(encodedContents) as T
                }

                Asn1Type.OBJECT_IDENTIFIER -> if (String::class.java == targetType) {
                    return oidToString(encodedContents) as T
                }

                Asn1Type.SEQUENCE -> {
                    val containerAnnotation = targetType.getAnnotation(
                        Asn1Class::class.java
                    )
                    if (containerAnnotation != null && containerAnnotation.type == Asn1Type.SEQUENCE) {
                        return parseSequence(dataValue, targetType)
                    }
                }

                Asn1Type.CHOICE -> {
                    val containerAnnotation = targetType.getAnnotation(
                        Asn1Class::class.java
                    )
                    if (containerAnnotation != null && containerAnnotation.type == Asn1Type.CHOICE) {
                        return parseChoice(dataValue, targetType)
                    }
                }

                else -> {}
            }
            throw Asn1DecodingException(
                "Unsupported conversion: ASN.1 $sourceType to ${targetType.name}"
            )
        }
    }
}