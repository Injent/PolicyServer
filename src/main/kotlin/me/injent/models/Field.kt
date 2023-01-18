package me.injent.models

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import me.injent.data.ResultOf
import me.injent.extensions.EnumNameSerializer
import me.injent.models.Resource.Companion.warResources
import me.injent.packets.Packet
import me.injent.utils.WeightedRandomList
import java.io.FileNotFoundException
import java.lang.IndexOutOfBoundsException
import kotlin.random.Random

@Serializable
data class Field(
    val x: Int = -1,
    val y: Int = -1,
    val name: String = "Unnamed",
    var captureProgress: Int = 0,
    val type: Type = Type.PLAINS,
    var owner: String? = null,
    var lastCaptured: String? = null,
    val resources: List<Resource> = randomResources(type),
    val buildings: List<Building> = mutableListOf(),
) {
    companion object {
        fun getStartField(player: Player, fields: Array<Array<Field>>): Field {
            val size = fields.size
            var x = Random.nextInt(0, size)
            var y = Random.nextInt(0, size)

            while (fields[y][x].owner != null) {
                x = Random.nextInt(0, size)
                y = Random.nextInt(0, size)
            }
            return Field(
                x = x,
                y = y,
                name = "${player.name} (Столица)",
                captureProgress = 3,
                owner = player.name,
                lastCaptured = player.name,
                buildings = mutableListOf(Building(Building.Type.HOUSE))
            )
        }

        private fun randomType(): Type {
            val weightedList = WeightedRandomList<Type>().apply {
                Type.values().forEach { type ->
                    addEntry(type, type.weight)
                }
            }

            return weightedList.randomEntry!!
        }

        fun randomResources(type: Type): List<Resource> {
            return type.resources.map { resourceType ->
                val count = Random.nextInt(10, resourceType.maxCount)
                Resource(count, resourceType)
            }.toMutableList()
        }

        @OptIn(ExperimentalSerializationApi::class)
        fun randomFields(size: Int, seed: Long): Array<Array<Field>> {
            val stream = this::class.java.classLoader.getResource("terrain_names.json")?.openStream()
                ?: throw FileNotFoundException("terrain_names.json")
            val names = Json.decodeFromStream<List<String>>(stream)

            val fields: Array<Array<Field?>> = Array(size) { Array(size) { null } }
            repeat(size) { x ->
                repeat(size) { y ->
                    fields[y][x] = Field(
                        x,
                        y,
                        name = names[Random.nextInt(0, names.size)],
                        type = randomType()
                    )
                }
            }
            return fields as Array<Array<Field>>
        }
    }

    val isCaptured
        get() = captureProgress >= 3

    fun capture(fields: Array<Array<Field>>, player: Player): ResultOf {
        if (owner != null) return ResultOf.Error("Этим полем владеет $owner")
        if (captureProgress > 0 && lastCaptured != player.name) return ResultOf.Error("Это поле захватывает $lastCaptured")
        if (type == Type.WATER) return ResultOf.Error("Здесь вода")
        if (!isNearFields(fields, player.name)) return ResultOf.Error("Территория далеко от вас")
        lastCaptured = player.name
        captureProgress++

        if (isCaptured) {
            owner = player.name
            player.fields++
        }
        return ResultOf.Success
    }

    fun mineResource(player: Player, type: Resource.Type): ResultOf {
        if (!resources.any { it.type == type && it.count > 0 }) return ResultOf.Error("Ресурс исчерпан")
        if (owner != player.name || !isCaptured) return ResultOf.Error("Не ваша территория")

        val resource = resources.find {
            it.type == type && it.count > 0
        }
        val index = resources.indexOf(resource)
        resource?.apply {
            if (count > 0)
                count--
            (resources as MutableList<Resource>)[index] = resource
            player.addResource(type)
        }
        return ResultOf.Success
    }

    fun build(player: Player, type: Building.Type): Packet? {
        if (owner == null) return alertPacket("Вы пока не можете строить здесь здания")
        if (this.type.slots == buildings.size) return alertPacket("Ограничение по зданиям")
        if (owner != player.name) return alertPacket("Вы не владеете этим местом")

        val enoughResources = player.isEnoughResourcesFor(type)
        val limit = player.buildings < player.houses * 2
        val canBuy = if (enoughResources) {
            if (type == Building.Type.HOUSE)
                true
            else limit
        } else false

        if (canBuy) {
            (buildings as MutableList<Building>).add(Building(type))
            for ((count, resourceType) in type.resources) {
                player.removeResource(resourceType, count)
            }
            if (type == Building.Type.HOUSE)
                player.houses++
            else
                player.buildings++
            return null
        }
        return if (!enoughResources) alertPacket("Недостаточно ресурсов для постройки", Sound.NOT_ENOUGH_RESOURCES)
        else alertPacket("Постройте жилой дом, чтобы строить еще")
    }

    fun attack(fields: Array<Array<Field>>, attacker: Player, attackedPlayer: Player?, attackedField: Field): Packet? {
        return if (true) {  //
            if (attackedPlayer == null) return alertPacket("Кого ты атакуешь?")
            if (attackedField.owner == attacker.name || attackedField.lastCaptured == attacker.name || attackedField.owner == null) return alertPacket("Зачем на пустую территорию нападать?")
            if (owner != attacker.name) return alertPacket("Не ваша база")
            if (!buildings.any { it.type == Building.Type.MILITARY_BASE }) return alertPacket("У вас нет военных баз для нападения")
            if (!attackedField.isNearFields(fields, attacker.name)) return alertPacket("Это поле далеко от вас")
            if (!attacker.isEnoughResourcesForWar()) return alertPacket("Не хватает ресурсов для нападения")

            warResources().forEach { resource -> attacker.removeResource(resource.type, resource.count) }
            if (attackedField.buildings.any { it.type == Building.Type.FORT }) {
                (attackedField.buildings as MutableList<Building>).remove(Building(Building.Type.FORT))
                attacker.buildings--
                attackedPlayer.buildings--
                (buildings as MutableList<Building>).remove(Building(Building.Type.MILITARY_BASE))
                return null
            }
            val capturedHouses = attackedField.buildings.filter { it.type == Building.Type.HOUSE }.size
            val capturedBuildings = attackedField.buildings.size - capturedHouses

            attacker.buildings += capturedBuildings - 1
            attacker.houses += capturedHouses

            attackedPlayer.buildings -= capturedBuildings
            attackedPlayer.houses -= capturedHouses
            attackedPlayer.fields -= 1

            attackedField.owner = attacker.name
            attackedField.lastCaptured = attacker.name

            // Clear attacker's field
            owner = null
            lastCaptured = null
            captureProgress = 0
            (buildings as MutableList<Building>).remove(Building(Building.Type.MILITARY_BASE))
            null
        } else alertPacket("Вы не объявили войну владельцу территории")
    }

    fun isNearFields(fields: Array<Array<Field>>, playerName: String): Boolean {
        val left = try {
            fields[y][x - 1].owner == playerName
        } catch (e: IndexOutOfBoundsException) {
            false
        }
        val right = try {
            fields[y][x + 1].owner == playerName
        } catch (e: IndexOutOfBoundsException) {
            false
        }
        val top = try {
            fields[y - 1][x].owner == playerName
        } catch (e: IndexOutOfBoundsException) {
            false
        }
        val bottom = try {
            fields[y + 1][x].owner == playerName
        } catch (e: IndexOutOfBoundsException) {
            false
        }

        return left || right || top || bottom
    }

    @Serializable(with = Type.EnumSerializer::class)
    enum class Type(val resources: List<Resource.Type>, val slots: Int = 3, val weight: Float = 1f) {
        WATER(emptyList(), 0, .05f),
        PLAINS(listOf(Resource.Type.WOOD, Resource.Type.STONE, Resource.Type.COAL), 4, .2f),
        FOREST(listOf(Resource.Type.WOOD, Resource.Type.STONE, Resource.Type.IRON), 3, .2f),
        DESERT(listOf(Resource.Type.IRON, Resource.Type.GOLD), 3, .15f),
        MOUNTAINS(listOf(Resource.Type.COAL, Resource.Type.STONE, Resource.Type.IRON), 2, .2f),
        MESA(listOf(Resource.Type.GOLD, Resource.Type.WOOD), 3, .1f),
        ISLAND(listOf(Resource.Type.CRYSTAL), 2, .05f),
        SWAMP(listOf(Resource.Type.COAL), 2, .05f);

        object EnumSerializer : EnumNameSerializer<Type>(Type::class)
    }
}