package com.sohan.diutransportschedule.db

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object JsonConverters {
    private val gson = Gson()
    fun listToJson(list: List<String>) = gson.toJson(list)
    fun jsonToList(json: String): List<String> {
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }
}