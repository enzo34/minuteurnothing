package fr.enzo.cachet45

import android.content.Context

/** Le peu d'état que l'appli conserve, dans un SharedPreferences. */
object Reglages {

    const val DUREE_PAR_DEFAUT_MIN = 45
    val DUREES_PROPOSEES = listOf(30, 45, 60)

    private const val FICHIER = "cachet45"
    private const val CLE_FIN = "fin_a"
    private const val CLE_DEBUT = "debut_a"
    private const val CLE_DUREE = "duree_min"
    private const val CLE_SONNERIE = "sonnerie"
    private const val CLE_VERSION_CANAL = "version_canal"

    private fun prefs(contexte: Context) =
        contexte.applicationContext.getSharedPreferences(FICHIER, Context.MODE_PRIVATE)

    /** Horodatage de fin du minuteur en cours, ou 0 s'il n'y en a pas. */
    fun finA(contexte: Context): Long = prefs(contexte).getLong(CLE_FIN, 0L)

    fun debutA(contexte: Context): Long = prefs(contexte).getLong(CLE_DEBUT, 0L)

    fun dureeMin(contexte: Context): Int =
        prefs(contexte).getInt(CLE_DUREE, DUREE_PAR_DEFAUT_MIN)

    fun definirDureeMin(contexte: Context, minutes: Int) {
        prefs(contexte).edit().putInt(CLE_DUREE, minutes.coerceIn(1, 240)).apply()
    }

    fun demarrerMinuteur(contexte: Context, debut: Long, fin: Long) {
        prefs(contexte).edit()
            .putLong(CLE_DEBUT, debut)
            .putLong(CLE_FIN, fin)
            .apply()
    }

    fun effacerMinuteur(contexte: Context) {
        prefs(contexte).edit()
            .putLong(CLE_DEBUT, 0L)
            .putLong(CLE_FIN, 0L)
            .apply()
    }

    // --- Sonnerie de fin -----------------------------------------------------

    /** URI de la sonnerie choisie, ou null pour l'alarme par défaut du téléphone. */
    fun sonnerie(contexte: Context): String? {
        val valeur = prefs(contexte).getString(CLE_SONNERIE, null)
        return if (valeur.isNullOrEmpty()) null else valeur
    }

    /**
     * Android fige le son d'un canal de notification à sa création : en changer
     * impose d'en recréer un neuf, d'où ce numéro de version dans son identifiant.
     */
    fun versionCanal(contexte: Context): Int = prefs(contexte).getInt(CLE_VERSION_CANAL, 1)

    fun definirSonnerie(contexte: Context, uri: String?) {
        if (sonnerie(contexte) == uri) return
        prefs(contexte).edit()
            .putString(CLE_SONNERIE, uri ?: "")
            .putInt(CLE_VERSION_CANAL, versionCanal(contexte) + 1)
            .apply()
    }
}
