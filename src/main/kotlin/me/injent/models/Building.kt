package me.injent.models

import kotlinx.serialization.Serializable
import me.injent.extensions.EnumNameSerializer

@Serializable
data class Building(
    val type: Type = Type.NONE,
) {
    @Serializable(with = Building.Type.EnumSerializer::class)
    enum class Type(val resources: List<Resource> = emptyList()) {
        NONE,
        SAWMILL(
            listOf(
                Resource(2, Resource.Type.WOOD),
                Resource(4, Resource.Type.STONE)
            )
        ),
        MINESHAFT(
            listOf(
                Resource(4, Resource.Type.WOOD),
                Resource(2, Resource.Type.STONE)
            )
        ),
        HOUSE(
            listOf(
                Resource(5, Resource.Type.WOOD),
            )
        ),
        LABORATORY(
            listOf(
                Resource(3, Resource.Type.STONE),
                Resource(2, Resource.Type.IRON)
            )
        ),
        BANK(
            listOf(
                Resource(3, Resource.Type.STONE),
                Resource(3, Resource.Type.IRON),
                Resource(5, Resource.Type.GOLD)
            )
        ),
        MILITARY_BASE(
            listOf(
                Resource(3, Resource.Type.IRON),
                Resource(4, Resource.Type.STONE)
            )
        ),
        FORT(
            listOf(
                Resource(6, Resource.Type.STONE),
            )
        );

        object EnumSerializer : EnumNameSerializer<Type>(Type::class)
    }
}