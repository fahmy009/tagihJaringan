package com.fahms.tagihjaringan.data.remote

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

class FlexibleBooleanAdapter : JsonDeserializer<Boolean> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): Boolean {
        if (json == null || json.isJsonNull) {
            return false
        }
        if (json.isJsonPrimitive) {
            val primitive = json.asJsonPrimitive
            if (primitive.isBoolean) {
                return primitive.asBoolean
            }
            if (primitive.isNumber) {
                // Any number present in bayar column (e.g. 0 or 95000) indicates customer is paid (Lunas)
                return true
            }
            if (primitive.isString) {
                val str = primitive.asString.trim()
                if (str.isEmpty() || str.equals("false", ignoreCase = true) || str.equals("null", ignoreCase = true)) {
                    return false
                }
                return true
            }
        }
        return false
    }
}

class FlexibleIntAdapter : JsonDeserializer<Int> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): Int {
        if (json == null || json.isJsonNull) {
            return 0
        }
        if (json.isJsonPrimitive) {
            val primitive = json.asJsonPrimitive
            if (primitive.isNumber) {
                return primitive.asInt
            }
            if (primitive.isString) {
                val cleanStr = primitive.asString.replace(".", "").replace(",", "").replace(" ", "").trim()
                return cleanStr.toIntOrNull() ?: 0
            }
        }
        return 0
    }
}
