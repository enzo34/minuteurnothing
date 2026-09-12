package fr.enzo.cachet45

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

enum class Etat { REPOS, EN_COURS, TERMINE }

/**
 * Toute la logique du minuteur. Volontairement sans service ni thread :
 * une alarme système à l'heure de fin suffit, et l'affichage du compte à
 * rebours est délégué au Chronometer du widget et de la notification, qui
 * décomptent tout seuls.
 */
object Minuteur {

    const val ACTION_DEMARRER = "fr.enzo.cachet45.DEMARRER"
    const val ACTION_ANNULER = "fr.enzo.cachet45.ANNULER"
    const val ACTION_FIN = "fr.enzo.cachet45.FIN"
    const val ACTION_ACQUITTER = "fr.enzo.cachet45.ACQUITTER"
    const val ACTION_TIC = "fr.enzo.cachet45.TIC"

    private const val CODE_ALARME = 100
    private const val CODE_ECRAN_ALARME = 101
    private const val CODE_TIC = 102

    fun etat(contexte: Context): Etat {
        val fin = Reglages.finA(contexte)
        return when {
            fin == 0L -> Etat.REPOS
            System.currentTimeMillis() < fin -> Etat.EN_COURS
            else -> Etat.TERMINE
        }
    }

    /** Millisecondes restantes (>= 0). */
    fun restantMs(contexte: Context): Long =
        (Reglages.finA(contexte) - System.currentTimeMillis()).coerceAtLeast(0L)

    /** Minutes affichées : on arrondit au-dessus, 45 → 44 → … → 1 → terminé. */
    fun minutesRestantes(contexte: Context): Int =
        ((restantMs(contexte) + 59_999L) / 60_000L).toInt()

    /** En deçà, une annulation est traitée comme une fausse manœuvre. */
    private const val FAUSSE_MANOEUVRE_MS = 120_000L

    fun demarrer(contexte: Context) {
        val maintenant = System.currentTimeMillis()
        val duree = Reglages.dureeMin(contexte)
        val fin = maintenant + duree * 60_000L
        Reglages.demarrerMinuteur(contexte, maintenant, fin)
        Historique.ajouter(contexte, Prise(maintenant, duree))
        programmerAlarme(contexte, fin)
        programmerTic(contexte)
        Notifications.afficherEnCours(contexte, fin)
        MinuteurWidget.rafraichir(contexte)
        MinuteurTile.rafraichir(contexte)
    }

    /** Rafraîchit le widget au changement de minute affichée, puis se replanifie. */
    fun tic(contexte: Context) {
        MinuteurWidget.rafraichir(contexte)
        MinuteurTile.rafraichir(contexte)
        if (etat(contexte) == Etat.EN_COURS) programmerTic(contexte)
    }

    /**
     * Annuler tout de suite signifie « je me suis trompé de bouton » : la prise
     * est retirée du journal. Annuler plus tard signifie « j'arrête le décompte »,
     * et le cachet, lui, a bien été pris : la prise reste enregistrée.
     */
    fun annuler(contexte: Context) {
        val debut = Reglages.debutA(contexte)
        if (debut > 0L && System.currentTimeMillis() - debut < FAUSSE_MANOEUVRE_MS) {
            Historique.supprimer(contexte, debut)
        }
        annulerAlarme(contexte)
        annulerTic(contexte)
        Reglages.effacerMinuteur(contexte)
        Notifications.toutEffacer(contexte)
        MinuteurWidget.rafraichir(contexte)
        MinuteurTile.rafraichir(contexte)
    }

    /** Appelé par l'alarme à l'échéance : on garde l'état TERMINE jusqu'à acquittement. */
    fun terminer(contexte: Context) {
        annulerTic(contexte)
        Notifications.effacerEnCours(contexte)
        Notifications.afficherTermine(contexte)
        vibrer(contexte)
        MinuteurWidget.rafraichir(contexte)
        MinuteurTile.rafraichir(contexte)
    }

    /** « J'ai vu » : remet le widget à zéro. */
    fun acquitter(contexte: Context) {
        annulerAlarme(contexte)
        annulerTic(contexte)
        Reglages.effacerMinuteur(contexte)
        Notifications.toutEffacer(contexte)
        MinuteurWidget.rafraichir(contexte)
        MinuteurTile.rafraichir(contexte)
    }

    /** Un seul appui : démarre si au repos, acquitte si terminé, sinon ne touche à rien. */
    fun appuiPrincipal(contexte: Context) {
        when (etat(contexte)) {
            Etat.REPOS -> demarrer(contexte)
            Etat.TERMINE -> acquitter(contexte)
            Etat.EN_COURS -> Unit
        }
    }

    /** Après un redémarrage du téléphone les alarmes sont perdues : on les repose. */
    fun restaurer(contexte: Context) {
        when (etat(contexte)) {
            Etat.EN_COURS -> {
                val fin = Reglages.finA(contexte)
                programmerAlarme(contexte, fin)
                programmerTic(contexte)
                Notifications.afficherEnCours(contexte, fin)
            }
            Etat.TERMINE -> Notifications.afficherTermine(contexte)
            Etat.REPOS -> Notifications.toutEffacer(contexte)
        }
        MinuteurWidget.rafraichir(contexte)
        MinuteurTile.rafraichir(contexte)
    }

    fun intentionDiffusee(contexte: Context, action: String, code: Int): PendingIntent =
        PendingIntent.getBroadcast(
            contexte,
            code,
            Intent(contexte, MinuteurReceiver::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun alarmManager(contexte: Context) =
        contexte.getSystemService(AlarmManager::class.java)

    private fun programmerAlarme(contexte: Context, fin: Long) {
        val gestionnaire = alarmManager(contexte) ?: return
        val declencheur = intentionDiffusee(contexte, ACTION_FIN, CODE_ALARME)
        val ecran = PendingIntent.getActivity(
            contexte,
            CODE_ECRAN_ALARME,
            Intent(contexte, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        // setAlarmClock est la seule variante que le système ne repousse jamais,
        // même en veille profonde ou en mode économie d'énergie.
        try {
            gestionnaire.setAlarmClock(AlarmManager.AlarmClockInfo(fin, ecran), declencheur)
            return
        } catch (_: SecurityException) {
            // L'utilisateur a retiré l'autorisation « alarmes exactes ».
        }
        try {
            gestionnaire.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fin, declencheur)
        } catch (_: SecurityException) {
            gestionnaire.set(AlarmManager.RTC_WAKEUP, fin, declencheur)
        }
    }

    private fun annulerAlarme(contexte: Context) {
        alarmManager(contexte)?.cancel(intentionDiffusee(contexte, ACTION_FIN, CODE_ALARME))
    }

    /**
     * Le widget affiche des minutes : inutile de le redessiner chaque seconde,
     * une alarme au prochain changement de minute suffit.
     */
    private fun programmerTic(contexte: Context) {
        val gestionnaire = alarmManager(contexte) ?: return
        val fin = Reglages.finA(contexte)
        val maintenant = System.currentTimeMillis()
        val minutes = minutesRestantes(contexte)
        if (minutes <= 1) return // la dernière minute est couverte par l'alarme de fin
        // 200 ms après la bascule, pour que le recalcul tombe bien sur la minute suivante.
        val prochain = (fin - (minutes - 1) * 60_000L + 200L).coerceAtLeast(maintenant + 1_000L)
        val declencheur = intentionDiffusee(contexte, ACTION_TIC, CODE_TIC)
        try {
            gestionnaire.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, prochain, declencheur)
        } catch (_: SecurityException) {
            gestionnaire.set(AlarmManager.RTC_WAKEUP, prochain, declencheur)
        }
    }

    private fun annulerTic(contexte: Context) {
        alarmManager(contexte)?.cancel(intentionDiffusee(contexte, ACTION_TIC, CODE_TIC))
    }

    /** Vibration explicite : elle marche même si les notifications sont refusées. */
    private fun vibrer(contexte: Context) {
        val vibreur = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            contexte.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            contexte.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        } ?: return
        if (!vibreur.hasVibrator()) return
        val motif = longArrayOf(0, 400, 200, 400, 200, 700)
        vibreur.vibrate(VibrationEffect.createWaveform(motif, -1))
    }
}
