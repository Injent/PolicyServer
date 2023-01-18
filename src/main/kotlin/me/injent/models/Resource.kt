package me.injent.models

import kotlinx.serialization.Serializable
import me.injent.extensions.EnumNameSerializer

@Serializable
data class Resource(
    var count: Int = 0,
    val type: Type = Type.NONE
) {
    val isMineable: Boolean
        get() = type != Type.WOOD

    companion object {
        fun startResources(): List<Resource> {
            val list = mutableListOf<Resource>()
            Type.values().forEach { type ->
                when (type) {
                    Type.WOOD -> list.add(Resource(type = type, count = 20))
                    Type.STONE -> list.add(Resource(type = type, count = 20))
                    Type.IRON -> list.add(Resource(type = Type.IRON, count = 5))
                    Type.NONE -> {}
                    else -> { list.add(Resource(type = type)) }
                }
            }
            return list
        }

        fun warResources(): List<Resource> {
            return listOf(Resource(4, Resource.Type.COAL))
        }
    }

    @Serializable(with = Type.EnumSerializer::class)
    enum class Type(val maxCount: Int = 20) {
        NONE(0),
        WOOD(60),
        STONE(60),
        COAL(40),
        IRON(40),
        CRYSTAL(14),
        GOLD(20);

        object EnumSerializer : EnumNameSerializer<Type>(Type::class)
    }
}
