package com.classy.securechat.data

import android.content.Context

object UserSharedPreferences {
    private const val PREFS_NAME = "chat_prefs"
    private const val KEY_USERNAME = "username"

    var myName: String = "Me"

    fun saveUser(context: Context, name: String) {
        myName = name
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_USERNAME, name)
            .apply()
    }

    fun isLoggedIn(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedName = prefs.getString(KEY_USERNAME, null)

        return if (savedName != null) {
            myName = savedName
            true
        } else {
            false
        }
    }

    fun logout(context: Context) {
        myName = "Me"
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }
}