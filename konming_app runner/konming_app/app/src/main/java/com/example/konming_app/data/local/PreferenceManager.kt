package com.example.konming_app.data.local

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

class PreferenceManager(private val context: Context) {
    companion object {
        private const val PREF_NAME = "konming_prefs"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USERNAME = "username"
        private const val KEY_FONT_SIZE = "font_size"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_SAVED_ACCOUNTS = "saved_accounts"
        private const val KEY_USER_IP_LOCATION = "user_ip_location"
    }

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    var isLoggedIn: Boolean
        get() = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_LOGGED_IN, value).apply()

    var userId: Int
        get() = prefs.getInt(KEY_USER_ID, -1)
        set(value) = prefs.edit().putInt(KEY_USER_ID, value).apply()

    var username: String?
        get() = prefs.getString(KEY_USERNAME, null)
        set(value) = prefs.edit().putString(KEY_USERNAME, value).apply()

    var fontSize: String
        get() = prefs.getString(KEY_FONT_SIZE, "medium") ?: "medium"
        set(value) = prefs.edit().putString(KEY_FONT_SIZE, value).apply()

    var language: String
        get() = prefs.getString(KEY_LANGUAGE, "zh") ?: "zh"
        set(value) = prefs.edit().putString(KEY_LANGUAGE, value).apply()

    fun saveLoginState(userId: Int, username: String) {
        this.userId = userId
        this.username = username
        this.isLoggedIn = true
    }

    fun getLoggedInUserId(): Int {
        return userId
    }

    fun getLoggedInUsername(): String? {
        return username
    }

    fun clearLoginState() {
        prefs.edit()
            .remove(KEY_IS_LOGGED_IN)
            .remove(KEY_USER_ID)
            .remove(KEY_USERNAME)
            .apply()
    }

    fun saveAccount(username: String, password: String) {
        val savedAccounts = getSavedAccounts().toMutableList()
        // 移除已有的同名账号
        savedAccounts.removeAll { it.first == username }
        savedAccounts.add(username to password)
        // 保存
        val jsonArray = JSONArray()
        for ((u, p) in savedAccounts) {
            val obj = JSONObject()
            obj.put("username", u)
            obj.put("password", p)
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_SAVED_ACCOUNTS, jsonArray.toString()).apply()
    }

    fun getSavedAccounts(): List<Pair<String, String>> {
        val accounts = mutableListOf<Pair<String, String>>()
        val jsonStr = prefs.getString(KEY_SAVED_ACCOUNTS, null)
        if (!jsonStr.isNullOrEmpty()) {
            try {
                val jsonArray = JSONArray(jsonStr)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val u = obj.getString("username")
                    val p = obj.getString("password")
                    accounts.add(u to p)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return accounts
    }

    fun removeAccount(username: String): Boolean {
        val savedAccounts = getSavedAccounts().toMutableList()
        val removed = savedAccounts.removeAll { it.first == username }
        // 保存
        val jsonArray = JSONArray()
        for ((u, p) in savedAccounts) {
            val obj = JSONObject()
            obj.put("username", u)
            obj.put("password", p)
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_SAVED_ACCOUNTS, jsonArray.toString()).apply()
        return removed
    }

    fun saveUserIpLocation(location: String) {
        prefs.edit().putString(KEY_USER_IP_LOCATION, location).apply()
    }

    fun getUserIpLocation(): String? {
        return prefs.getString(KEY_USER_IP_LOCATION, null)
    }
}