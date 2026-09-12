package fr.enzo.cachet45

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import java.util.Date

/**
 * Le widget d'accueil : un seul appui démarre le minuteur.
 *
 * Look Nothing OS : pilule anthracite, gros chiffres en matrice de points,
 * étiquette en petites capitales espacées en dessous.
 */
class MinuteurWidget : AppWidgetProvider() {

    override fun onUpdate(
        contexte: Context,
        gestionnaire: AppWidgetManager,
        identifiants: IntArray
    ) {
        val vues = construire(contexte)
        identifiants.forEach { gestionnaire.updateAppWidget(it, vues) }
    }

    override fun onEnabled(contexte: Context) {
        super.onEnabled(contexte)
        // Un widget vient d'être posé : s'il y a un minuteur en cours, on le rattrape.
        Minuteur.restaurer(contexte)
    }

    companion object {

        private const val CODE_DEMARRER = 300
        private const val CODE_ACQUITTER = 301
        private const val CODE_OUVRIR = 302

        /** Taille d'un point du widget, en dp. */
        private const val PAS_DP = 5.2f
        private const val REMPLISSAGE = 0.74f

        fun rafraichir(contexte: Context) {
            val gestionnaire = AppWidgetManager.getInstance(contexte) ?: return
            val identifiants = gestionnaire.getAppWidgetIds(
                ComponentName(contexte, MinuteurWidget::class.java)
            )
            if (identifiants.isEmpty()) return
            val vues = construire(contexte)
            identifiants.forEach { gestionnaire.updateAppWidget(it, vues) }
        }

        fun construire(contexte: Context): RemoteViews {
            val vues = RemoteViews(contexte.packageName, R.layout.widget_minuteur)
            val blanc = ContextCompat.getColor(contexte, R.color.blanc)
            val gris = ContextCompat.getColor(contexte, R.color.gris)
            val rouge = ContextCompat.getColor(contexte, R.color.rouge)
            val vert = ContextCompat.getColor(contexte, R.color.vert)

            when (Minuteur.etat(contexte)) {
                Etat.REPOS -> {
                    val duree = Reglages.dureeMin(contexte)
                    points(contexte, vues, duree.toString(), blanc)
                    etiquettes(
                        vues,
                        gauche = contexte.getString(R.string.widget_gauche_repos),
                        couleurGauche = blanc,
                        droite = contexte.getString(R.string.widget_droite_repos, duree),
                        couleurDroite = gris
                    )
                    vues.setContentDescription(
                        R.id.widget_racine,
                        contexte.getString(R.string.widget_accessibilite_repos, duree)
                    )
                    vues.setOnClickPendingIntent(
                        R.id.widget_racine,
                        Minuteur.intentionDiffusee(
                            contexte,
                            Minuteur.ACTION_DEMARRER,
                            CODE_DEMARRER
                        )
                    )
                }

                Etat.EN_COURS -> {
                    val minutes = Minuteur.minutesRestantes(contexte)
                    points(contexte, vues, minutes.toString(), blanc)
                    etiquettes(
                        vues,
                        gauche = contexte.getString(R.string.widget_gauche_en_cours),
                        couleurGauche = rouge,
                        droite = heureDeFin(contexte),
                        couleurDroite = gris
                    )
                    vues.setContentDescription(
                        R.id.widget_racine,
                        contexte.getString(R.string.widget_accessibilite_en_cours, minutes)
                    )
                    vues.setOnClickPendingIntent(R.id.widget_racine, ouvrirAppli(contexte))
                }

                Etat.TERMINE -> {
                    points(contexte, vues, "OK", vert)
                    etiquettes(
                        vues,
                        gauche = contexte.getString(R.string.widget_gauche_termine),
                        couleurGauche = vert,
                        droite = contexte.getString(R.string.widget_droite_termine),
                        couleurDroite = gris
                    )
                    vues.setContentDescription(
                        R.id.widget_racine,
                        contexte.getString(R.string.widget_accessibilite_termine)
                    )
                    vues.setOnClickPendingIntent(
                        R.id.widget_racine,
                        Minuteur.intentionDiffusee(
                            contexte,
                            Minuteur.ACTION_ACQUITTER,
                            CODE_ACQUITTER
                        )
                    )
                }
            }
            return vues
        }

        private fun points(
            contexte: Context,
            vues: RemoteViews,
            texte: String,
            couleur: Int
        ) {
            val densite = contexte.resources.displayMetrics.density
            val pas = PAS_DP * densite
            vues.setImageViewBitmap(
                R.id.widget_points,
                Matrice.bitmap(texte, pas, pas * REMPLISSAGE, couleur)
            )
        }

        private fun etiquettes(
            vues: RemoteViews,
            gauche: String,
            couleurGauche: Int,
            droite: String,
            couleurDroite: Int
        ) {
            vues.setTextViewText(R.id.widget_titre, gauche)
            vues.setTextColor(R.id.widget_titre, couleurGauche)
            vues.setTextViewText(R.id.widget_bas, droite)
            vues.setTextColor(R.id.widget_bas, couleurDroite)
        }

        /** Heure à laquelle on pourra manger, au format du téléphone. */
        fun heureDeFin(contexte: Context): String =
            android.text.format.DateFormat.getTimeFormat(contexte)
                .format(Date(Reglages.finA(contexte)))

        private fun ouvrirAppli(contexte: Context): PendingIntent = PendingIntent.getActivity(
            contexte,
            CODE_OUVRIR,
            Intent(contexte, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
