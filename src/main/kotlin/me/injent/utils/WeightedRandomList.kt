package me.injent.utils

import kotlin.random.Random


class WeightedRandomList<T : Any?> {
    private inner class Entry(
        val accumulatedWeight: Float = 0f,
        val `object`: T? = null
    )

    private val entries: MutableList<Entry> = ArrayList()
    private var accumulatedWeight = 0f
    var seed: Long? = null

    fun addEntry(`object`: T, weight: Float) {
        accumulatedWeight += weight
        val e = Entry(
            accumulatedWeight,
            `object`
        )
        entries.add(e)
    }

    val randomEntry: T?
        get() {
            val r: Float = if (seed != null)
                Random(seed!!).nextFloat() * accumulatedWeight
            else
                Random.nextFloat() * accumulatedWeight

            for (entry in entries) {
                if (entry.accumulatedWeight >= r) {
                    return entry.`object`
                }
            }
            return null
        }
}