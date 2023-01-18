package me.injent.extensions

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.reflect.KClass

open class EnumNameSerializer<E>(
    private val kClass: KClass<E>
) : KSerializer<E> where E : Enum<E>  {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Enum", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: E) {
        encoder.encodeString(value.name.lowercase())
    }

    override fun deserialize(decoder: Decoder): E =
        decoder.decodeString().let { value ->
            kClass.java.enumConstants.firstOrNull { it.name.lowercase() == value } ?: run {
                throw IllegalStateException("Cannot find enum with label $value")
            }
        }
}