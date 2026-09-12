package fr.enzo.cachet45

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object Notifications {

    private const val CANAL_EN_COURS = "minuteur_en_cours"
    private const val CANAL_TERMINE = "minuteur_termine"

    private const val ID_EN_COURS = 1
    private const val ID_TERMINE = 2

    private const val CODE_OUVRIR = 200
    private const val CODE_ANNULER = 201
    private const val CODE_ACQUITTER = 202

    private fun manager(contexte: Context) =
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

    /** Notification discrète qui décompte toute seule dans la barre d'état. */
    fun afficherEnCours(contexte: Context, fin: Long) {
        creerCanaux(contexte)
        val notification = NotificationCompat.Builder(contexte, CANAL_EN_COURS)
            .setSmallIcon(R.drawable.ic_pilule)
            .setContentTitle(contexte.getString(R.string.notif_en_cours_titre))
            .setContentText(contexte.getString(R.string.notif_en_cours_texte))
            .setWhen(fin)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(ouvrirAppli(contexte))
            .addAction(
                0,
                contexte.getString(R.string.annuler),
                Minuteur.intentionDiffusee(contexte, Minuteur.ACTION_ANNULER, CODE_ANNULER)
            )
            .build()
        notifier(contexte, ID_EN_COURS, notification)
    }

    fun afficherTermine(contexte: Context) {
        creerCanaux(contexte)
        val notification = NotificationCompat.Builder(contexte, CANAL_TERMINE)
            .setSmallIcon(R.drawable.ic_pilule)
            .setContentTitle(contexte.getString(R.string.notif_termine_titre))
            .setContentText(contexte.getString(R.string.notif_termine_texte))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(ouvrirAppli(contexte))
            .setDeleteIntent(
                Minuteur.intentionDiffusee(contexte, Minuteur.ACTION_ACQUITTER, CODE_ACQUITTER)
            )
            .addAction(
                0,
                contexte.getString(R.string.ok),
                Minuteur.intentionDiffusee(contexte, Minuteur.ACTION_ACQUITTER, CODE_ACQUITTER)
            )
            .build()
        notifier(contexte, ID_TERMINE, notification)
    }

    fun effacerEnCours(contexte: Context) {
        NotificationManagerCompat.from(contexte).cancel(ID_EN_COURS)
    }

    fun toutEffacer(contexte: Context) {
        NotificationManagerCompat.from(contexte).apply {
            cancel(ID_EN_COURS)
            cancel(ID_TERMINE)
        }
    }

    /**
     * Sans l'autorisation « notifications » l'appel est simplement ignoré :
     * le widget et la vibration restent la source de vérité.
     */
    private fun notifier(contexte: Context, id: Int, notification: android.app.Notification) {
        try {
            NotificationManagerCompat.from(contexte).notify(id, notification)
        } catch (_: SecurityException) {
            // Autorisation refusée, on continue sans notification.
        }
    }
}
