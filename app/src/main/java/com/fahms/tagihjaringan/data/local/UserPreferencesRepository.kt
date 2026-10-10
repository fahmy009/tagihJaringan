package com.fahms.tagihjaringan.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "tagih_jaringan_prefs")

data class UserSettings(
    val mikrotikIp: String = "",
    val mikrotikUser: String = "",
    val mikrotikPass: String = "",
    val filterProfile: String = "",
    val defaultNominal: Int = 95000
)

data class UserSession(
    val username: String = "",
    val role: String = "ADMIN", // ADMIN, MITRA, COLLECTOR
    val assignedProfile: String = "",
    val name: String = "",
    val isLoggedIn: Boolean = false
)

class UserPreferencesRepository(private val context: Context) {

    companion object {
        val MIKROTIK_IP_KEY = stringPreferencesKey("mikrotik_ip")
        val MIKROTIK_USER_KEY = stringPreferencesKey("mikrotik_user")
        val MIKROTIK_PASS_KEY = stringPreferencesKey("mikrotik_pass")
        val FILTER_PROFILE_KEY = stringPreferencesKey("filter_profile")
        val DEFAULT_NOMINAL_KEY = intPreferencesKey("default_nominal")

        val SESSION_USERNAME_KEY = stringPreferencesKey("session_username")
        val SESSION_ROLE_KEY = stringPreferencesKey("session_role")
        val SESSION_PROFILE_KEY = stringPreferencesKey("session_profile")
        val SESSION_NAME_KEY = stringPreferencesKey("session_name")
        val SESSION_LOGGED_IN_KEY = booleanPreferencesKey("session_logged_in")
    }

    val userSettingsFlow: Flow<UserSettings> = context.dataStore.data
        .map { preferences ->
            UserSettings(
                mikrotikIp = preferences[MIKROTIK_IP_KEY] ?: "",
                mikrotikUser = preferences[MIKROTIK_USER_KEY] ?: "",
                mikrotikPass = preferences[MIKROTIK_PASS_KEY] ?: "",
                filterProfile = preferences[FILTER_PROFILE_KEY] ?: "",
                defaultNominal = preferences[DEFAULT_NOMINAL_KEY] ?: 95000
            )
        }

    val userSessionFlow: Flow<UserSession> = context.dataStore.data
        .map { preferences ->
            UserSession(
                username = preferences[SESSION_USERNAME_KEY] ?: "",
                role = preferences[SESSION_ROLE_KEY] ?: "ADMIN",
                assignedProfile = preferences[SESSION_PROFILE_KEY] ?: "",
                name = preferences[SESSION_NAME_KEY] ?: "",
                isLoggedIn = preferences[SESSION_LOGGED_IN_KEY] ?: false
            )
        }

    suspend fun saveSettings(settings: UserSettings) {
        context.dataStore.edit { preferences ->
            preferences[MIKROTIK_IP_KEY] = settings.mikrotikIp
            preferences[MIKROTIK_USER_KEY] = settings.mikrotikUser
            preferences[MIKROTIK_PASS_KEY] = settings.mikrotikPass
            preferences[FILTER_PROFILE_KEY] = settings.filterProfile
            preferences[DEFAULT_NOMINAL_KEY] = settings.defaultNominal
        }
    }

    suspend fun saveUserSession(session: UserSession) {
        context.dataStore.edit { preferences ->
            preferences[SESSION_USERNAME_KEY] = session.username
            preferences[SESSION_ROLE_KEY] = session.role
            preferences[SESSION_PROFILE_KEY] = session.assignedProfile
            preferences[SESSION_NAME_KEY] = session.name
            preferences[SESSION_LOGGED_IN_KEY] = session.isLoggedIn
        }
    }

    suspend fun clearUserSession() {
        context.dataStore.edit { preferences ->
            preferences[SESSION_LOGGED_IN_KEY] = false
            preferences[SESSION_USERNAME_KEY] = ""
            preferences[SESSION_ROLE_KEY] = "ADMIN"
            preferences[SESSION_PROFILE_KEY] = ""
            preferences[SESSION_NAME_KEY] = ""
        }
    }
}
