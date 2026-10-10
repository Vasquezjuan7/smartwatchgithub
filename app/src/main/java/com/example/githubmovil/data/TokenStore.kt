package com.example.githubmovil.data

import android.content.Context

/**
 * Guarda el token OAuth del usuario y los PRs ya vistos en el almacenamiento privado de la app
 * (solo accesible por esta app; el backup está desactivado en el manifest).
 */
class TokenStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("github_auth", Context.MODE_PRIVATE)

    var token: String?
        get() = prefs.getString(KEY_TOKEN, null)
        set(value) {
            prefs.edit().apply {
                if (value == null) {
                    remove(KEY_TOKEN)
                    remove(KEY_SEEN) // al cerrar sesión olvidamos también los PRs vistos
                } else {
                    putString(KEY_TOKEN, value)
                }
            }.apply()
        }

    /**
     * PRs que el usuario ya conoce ("owner/repo#123"). `null` = todavía no se ha sincronizado nunca,
     * para no avisar de todos los PRs existentes la primera vez.
     */
    var seenPrs: Set<String>?
        get() = prefs.getStringSet(KEY_SEEN, null)?.toSet()
        set(value) {
            prefs.edit().apply {
                if (value == null) remove(KEY_SEEN) else putStringSet(KEY_SEEN, value.toSet())
            }.apply()
        }

    private companion object {
        const val KEY_TOKEN = "access_token"
        const val KEY_SEEN = "seen_prs"
    }
}
