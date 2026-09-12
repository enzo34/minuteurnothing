package fr.enzo.cachet45

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

/** Une prise de cachet : quand, et pour quel délai de jeûne. */
data class Prise(val debut: Long, val dureeMin: Int)

/**
 * Le journal des prises, en JSON dans les préférences.
 *
 * Une prise par jour pendant dix ans tient dans moins de 200 Ko : une base de
 * données serait disproportionnée, et l'appli garde ainsi zéro dépendance.
 */
object Historique {

    private const val FICHIER = "cachet45"
    private const val CLE = "historique"
    private const val MAXIMUM = 5000

    private const val CHAMP_DEBUT = "d"
    private const val CHAMP_DUREE = "m"

    private fun prefs(contexte: Context) =
        contexte.applicationContext.getSharedPreferences(FICHIER, Context.MODE_PRIVATE)

    /** De la plus récente à la plus ancienne. */
    fun toutes(contexte: Context): List<Prise> {
        val brut = prefs(contexte).getString(CLE, null) ?: return emptyList()
        val prises = ArrayList<Prise>()
        try {
            val tableau = JSONArray(brut)
            for (index in 0 until tableau.length()) {
                val objet = tableau.optJSONObject(index) ?: continue
                val debut = objet.optLong(CHAMP_DEBUT, 0L)
                if (debut > 0L) {
                    prises.add(
                        Prise(debut, objet.optInt(CHAMP_DUREE, Reglages.DUREE_PAR_DEFAUT_MIN))
                    )
                }
            }
        } catch (_: Exception) {
            // Journal illisible : on repart d'une liste vide plutôt que de planter.
            return emptyList()
        }
        prises.sortByDescending { it.debut }
        return prises
    }

    private fun enregistrer(contexte: Context, prises: List<Prise>) {
        val tableau = JSONArray()
        prises.take(MAXIMUM).forEach { prise ->
            tableau.put(
                JSONObject()
                    .put(CHAMP_DEBUT, prise.debut)
                    .put(CHAMP_DUREE, prise.dureeMin)
            )
        }
        prefs(contexte).edit().putString(CLE, tableau.toString()).apply()
    }

    fun ajouter(contexte: Context, prise: Prise) {
        val prises = ArrayList(toutes(contexte))
        prises.add(prise)
        prises.sortByDescending { it.debut }
        enregistrer(contexte, prises)
    }

    fun supprimer(contexte: Context, debut: Long) {
        enregistrer(contexte, toutes(contexte).filter { it.debut != debut })
    }

    fun derniere(contexte: Context): Prise? = toutes(contexte).firstOrNull()

    // Les vues chargent la liste une fois puis la filtrent : inutile de relire
    // et de reparser le journal à chaque question.

    /** Numéros des jours du mois comportant au moins une prise. */
    fun joursDuMois(prises: List<Prise>, annee: Int, mois: Int): Set<Int> {
        val jours = HashSet<Int>()
        val calendrier = Calendar.getInstance()
        prises.forEach { prise ->
            calendrier.timeInMillis = prise.debut
            if (calendrier.get(Calendar.YEAR) == annee &&
                calendrier.get(Calendar.MONTH) == mois
            ) {
                jours.add(calendrier.get(Calendar.DAY_OF_MONTH))
            }
        }
        return jours
    }

    fun duJour(prises: List<Prise>, annee: Int, mois: Int, jour: Int): List<Prise> {
        val calendrier = Calendar.getInstance()
        return prises.filter { prise ->
            calendrier.timeInMillis = prise.debut
            calendrier.get(Calendar.YEAR) == annee &&
                calendrier.get(Calendar.MONTH) == mois &&
                calendrier.get(Calendar.DAY_OF_MONTH) == jour
        }
    }
}
