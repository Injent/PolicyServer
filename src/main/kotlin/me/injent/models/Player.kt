package me.injent.models

import kotlinx.serialization.Serializable
import me.injent.models.Resource.Companion.startResources
import me.injent.models.Resource.Companion.warResources

@Serializable
data class Player(
    val name: String = "",
    val resources: List<Resource> = startResources(),
    var fields: Int = 1,
    var houses: Int = 0,
    var buildings: Int = 0,
    val rgb: Int = 0,
    val notification: String? = null,
    val wars: List<String> = mutableListOf(),
    val disconnected: Boolean = false,
) {
    fun isEnoughResourcesForWar(): Boolean {
        var availableResCount = 0
        val requiredResourcesForWar = warResources()

        for ((count, type) in requiredResourcesForWar) {
            val playerResource = resources.find {
                type == it.type
            }
            if (count <= playerResource!!.count)
                availableResCount++
        }
        return availableResCount >= requiredResourcesForWar.size
    }

    fun isEnoughResourcesFor(buildingType: Building.Type): Boolean {
        var availableResCount = 0

        buildingType.resources.forEach { requiredResource ->
            val playerResource = resources.find {
                requiredResource.type == it.type
            }
            if (requiredResource.count <= playerResource!!.count)
                availableResCount++
        }
        return availableResCount >= buildingType.resources.size
    }

    fun isCanManipulate(field: Field): Boolean {
        if (field.isCaptured && field.owner != name) return false
        if (!field.isCaptured && field.lastCaptured != name && field.captureProgress > 0) return false
        return true
    }

    fun addResource(type: Resource.Type, count: Int = 1) {
        val d = resources.find {
            it.type == type
        }
        d?.let { resource ->
            val index = resources.indexOf(resource)
            (resources as MutableList<Resource>)[index] = Resource(resource.count + count, type)
        }
    }

    fun removeResource(type: Resource.Type, count: Int = 1) {
        val d = resources.find {
            it.type == type
        }

        d?.let { resource ->
            val index = resources.indexOf(resource)
            (resources as MutableList<Resource>)[index] = Resource(resource.count - count, type)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Player

        if (name != other.name) return false
        if (resources != other.resources) return false
        if (fields != other.fields) return false
        if (houses != other.houses) return false
        if (buildings != other.buildings) return false
        if (rgb != other.rgb) return false
        if (notification != other.notification) return false
        if (wars != other.wars) return false
        if (disconnected != other.disconnected) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + resources.hashCode()
        result = 31 * result + fields
        result = 31 * result + houses
        result = 31 * result + buildings
        result = 31 * result + rgb
        result = 31 * result + (notification?.hashCode() ?: 0)
        result = 31 * result + wars.hashCode()
        result = 31 * result + disconnected.hashCode()
        return result
    }
}