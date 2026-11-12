package com.gglee.baziyuce.data

import com.gglee.baziyuce.engine.Gender
import org.json.JSONObject
import java.util.UUID

/** 多人员档案 — 供 iOS 对齐的共享数据模型。 */
data class PersonProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val birthYear: Int,
    val birthMonth: Int,
    val birthDay: Int,
    val birthShiChenIndex: Int,
    val gender: Gender,
    val updatedAtMs: Long = System.currentTimeMillis(),
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("birthYear", birthYear)
        put("birthMonth", birthMonth)
        put("birthDay", birthDay)
        put("birthShiChenIndex", birthShiChenIndex)
        put("gender", gender.name)
        put("updatedAtMs", updatedAtMs)
    }

    companion object {
        fun fromJson(o: JSONObject): PersonProfile = PersonProfile(
            id = o.getString("id"),
            name = o.getString("name"),
            birthYear = o.getInt("birthYear"),
            birthMonth = o.getInt("birthMonth"),
            birthDay = o.getInt("birthDay"),
            birthShiChenIndex = o.getInt("birthShiChenIndex"),
            gender = runCatching { Gender.valueOf(o.getString("gender")) }.getOrDefault(Gender.MALE),
            updatedAtMs = o.optLong("updatedAtMs", System.currentTimeMillis()),
        )
    }
}
