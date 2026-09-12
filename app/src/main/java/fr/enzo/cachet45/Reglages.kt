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
    private const val CLE_DERNIERE_PRISE = "derniere_prise"

    private fun prefs(contexte: Context) =
        contexte.applicationContext.getSharedPreferences(FICHIER, Context.MODE_PRIVATE)

    /** Horodatage de fin du minuteur en cours, ou 0 s'il n'y en a pas. */
    fun finA(contexte: Context): Long = prefs(contexte).getLong(CLE_FIN, 0L)

    fun debutA(contexte: Context): Long = prefs(contexte).getLong(CLE_DEBUT, 0L)

    fun dernierePrise(contexte: Context): Long = prefs(contexte).getLong(CLE_DERNIERE_PRISE, 0L)

    fun dureeMin(contexte: Context): Int =
        prefs(contexte).getInt(CLE_DUREE, DUREE_PAR_DEFAUT_MIN)

    fun definirDureeMin(contexte: Context, minutes: Int) {
        prefs(contexte).edit().putInt(CLE_DUREE, minutes.coerceIn(1, 240)).apply()
    }

    fun enregistrerPrise(contexte: Context, debut: Long, fin: Long) {
        prefs(contexte).edit()
            .putLong(CLE_DEBUT, debut)
            .putLong(CLE_FIN, fin)
            .putLong(CLE_DERNIERE_PRISE, debut)
            .apply()
    }

    /** Efface le minuteur courant sans toucher à l'historique de la dernière prise. */
    fun effacerMinuteur(contexte: Context) {
        prefs(contexte).edit()
            .putLong(CLE_DEBUT, 0L)
            .putLong(CLE_FIN, 0L)
            .apply()
    }
}
