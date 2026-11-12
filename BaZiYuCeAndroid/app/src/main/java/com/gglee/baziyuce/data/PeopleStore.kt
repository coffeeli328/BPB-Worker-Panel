package com.gglee.baziyuce.data

import android.content.Context
import org.json.JSONArray

/** SharedPreferences JSON 持久化人员档案。 */
class PeopleStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun loadPeople(): List<PersonProfile> {
        val raw = prefs.getString(KEY_PEOPLE, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    add(PersonProfile.fromJson(arr.getJSONObject(i)))
                }
            }
        }.getOrDefault(emptyList())
    }

    fun savePeople(people: List<PersonProfile>) {
        val arr = JSONArray()
        people.forEach { arr.put(it.toJson()) }
        prefs.edit().putString(KEY_PEOPLE, arr.toString()).apply()
    }

    fun loadActiveId(): String? = prefs.getString(KEY_ACTIVE, null)

    fun saveActiveId(id: String?) {
        prefs.edit().putString(KEY_ACTIVE, id).apply()
    }

    companion object {
        private const val PREFS = "bazi_people_v1"
        private const val KEY_PEOPLE = "people"
        private const val KEY_ACTIVE = "activeId"
    }
}
