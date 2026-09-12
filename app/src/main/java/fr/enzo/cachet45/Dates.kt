package fr.enzo.cachet45

import android.content.Context
import android.text.format.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Mise en forme des dates, partagée par l'écran principal et l'historique. */
object Dates {

    /** Heure au format du téléphone (24 h ou AM/PM selon ses réglages). */
    fun heure(contexte: Context, horodatage: Long): String =
        DateFormat.getTimeFormat(contexte).format(Date(horodatage))

    /** « aujourd'hui », « hier », sinon « lun. 8 sept. ». */
    fun jourRelatif(contexte: Context, horodatage: Long): String = when {
        memeJour(horodatage, System.currentTimeMillis()) ->
            contexte.getString(R.string.jour_aujourdhui)

        memeJour(horodatage, System.currentTimeMillis() - UN_JOUR_MS) ->
            contexte.getString(R.string.jour_hier)

        else -> formater(horodatage, if (memeAnnee(horodatage)) "EEE d MMM" else "d MMM yyyy")
    }

    /** « lundi 8 septembre », pour le détail d'une journée. */
    fun jourComplet(horodatage: Long): String =
        formater(horodatage, "EEEE d MMMM").replaceFirstChar { it.uppercase() }

    /** « Septembre 2026 », pour l'en-tête du calendrier. */
    fun moisEtAnnee(annee: Int, mois: Int): String {
        val calendrier = Calendar.getInstance().apply {
            clear()
            set(annee, mois, 1)
        }
        return SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            .format(calendrier.time)
    }

    private const val UN_JOUR_MS = 24L * 60L * 60L * 1000L

    private fun formater(horodatage: Long, motif: String): String =
        SimpleDateFormat(motif, Locale.getDefault()).format(Date(horodatage))

    private fun memeJour(premier: Long, second: Long): Boolean {
        val a = Calendar.getInstance().apply { timeInMillis = premier }
        val b = Calendar.getInstance().apply { timeInMillis = second }
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
            a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
    }

    private fun memeAnnee(horodatage: Long): Boolean {
        val a = Calendar.getInstance().apply { timeInMillis = horodatage }
        return a.get(Calendar.YEAR) == Calendar.getInstance().get(Calendar.YEAR)
    }
}
