package me.injent.extensions

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

fun String.extractParam(param: String): String {
    if (!contains(param)) return "none"
    return substringAfter("$param=").substringBefore(";")
}

@OptIn(ExperimentalSerializationApi::class)
val jsonFormat = Json { explicitNulls = false }