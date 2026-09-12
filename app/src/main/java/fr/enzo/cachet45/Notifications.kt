package fr.enzo.cachet45

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.media.AudioAttributes
import android.media.RingtoneManager

/**
 * Uniquement des API du framework : l'appli n'a ainsi aucune dépendance
 * externe, ce qui la rend compilable partout et l'APK minuscule.
 */
object Notifications {

    private const val CANAL_EN_COURS = "minuteur_en_cours"
    private const val CANAL_TERMINE = "minuteur_termine"

    private const val ID_EN_COURS = 1
    private const val ID_TERMINE = 2

    private const val CODE_OUVRIR = 200
    private const val CODE_ANNULER = 201
    private const val CODE_ACQUITTER = 202

    private fun manager(contexte: Context): NotificationManager? =
        contexte.getSystemService(NotificationManager::class.java)

    private fun creerCanaux(contexte: Context) {
        val gestionnaire = manager(contexte) ?: return

        val enCours = NotificationChannel(
            CANAL_EN_COURS,
            contexte.getString(R.string.canal_en_cours),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = contexte.getString(R.string.canal_en_cours_desc)
            setShowBadge(false)
            enableVibration(false)
            setSound(null, null)
        }

        val termine = NotificationChannel(
            CANAL_TERMINE,
            contexte.getString(R.string.canal_termine),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = contexte.getString(R.string.canal_termine_desc)
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 400, 200, 400, 200, 700)
            val son = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            setSound(
                son,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
        }

        gestionnaire.createNotificationChannel(enCours)
        gestionnaire.createNotificationChannel(termine)
    }

    private fun ouvrirAppli(contexte: Context): PendingIntent = PendingIntent.getActivity(
        contexte,
        CODE_OUVRIR,
        Intent(contexte, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun action(
        contexte: Context,
        libelle: String,
        intention: PendingIntent
    ): Notification.Action = Notification.Action.Builder(
        Icon.createWithResource(contexte, R.drawable.ic_pilule),
        libelle,
        intention
    ).build()

    /** Notification discrète qui décompte toute seule dans la barre d'état. */
    fun afficherEnCours(contexte: Context, fin: Long) {
        creerCanaux(contexte)
        val notification = Notification.Builder(contexte, CANAL_EN_COURS)
            .setSmallIcon(R.drawable.ic_pilule)
            .setContentTitle(contexte.getString(R.string.notif_en_cours_titre))
            .setContentText(contexte.getString(R.string.notif_en_cours_texte))
            .setWhen(fin)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(Notification.CATEGORY_PROGRESS)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setContentIntent(ouvrirAppli(contexte))
            .addAction(
                action(
                    contexte,
                    contexte.getString(R.string.annuler),
                    Minuteur.intentionDiffusee(contexte, Minuteur.ACTION_ANNULER, CODE_ANNULER)
                )
            )
            .build()
        notifier(contexte, ID_EN_COURS, notification)
    }

    fun afficherTermine(contexte: Context) {
        creerCanaux(contexte)
        val acquitter =
            Minuteur.intentionDiffusee(contexte, Minuteur.ACTION_ACQUITTER, CODE_ACQUITTER)
        val notification = Notification.Builder(contexte, CANAL_TERMINE)
            .setSmallIcon(R.drawable.ic_pilule)
            .setContentTitle(contexte.getString(R.string.notif_termine_titre))
            .setContentText(contexte.getString(R.string.notif_termine_texte))
            .setCategory(Notification.CATEGORY_ALARM)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(ouvrirAppli(contexte))
            .setDeleteIntent(acquitter)
            .addAction(action(contexte, contexte.getString(R.string.ok), acquitter))
            .build()
        notifier(contexte, ID_TERMINE, notification)
    }

    fun effacerEnCours(contexte: Context) {
        manager(contexte)?.cancel(ID_EN_COURS)
    }

    fun toutEffacer(contexte: Context) {
        manager(contexte)?.apply {
            cancel(ID_EN_COURS)
            cancel(ID_TERMINE)
        }
    }

    /**
     * Sans l'autorisation « notifications » l'appel est simplement ignoré :
     * le widget et la vibration restent la source de vérité.
     */
    private fun notifier(contexte: Context, identifiant: Int, notification: Notification) {
        try {
            manager(contexte)?.notify(identifiant, notification)
        } catch (_: SecurityException) {
            // Autorisation refusée, on continue sans notification.
        }
    }
}
