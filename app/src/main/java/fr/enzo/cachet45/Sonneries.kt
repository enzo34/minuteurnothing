package fr.enzo.cachet45

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri

/** Choix, mémorisation et écoute de la sonnerie de fin. */
object Sonneries {

    private var apercu: Ringtone? = null

    private val attributsAlarme: AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    /** La sonnerie choisie, ou l'alarme par défaut du téléphone. */
    fun uri(contexte: Context): Uri? {
        val choisie = Reglages.sonnerie(contexte)
        if (choisie != null) return Uri.parse(choisie)
        return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
    }

    /** Libellé lisible, pour l'écran des réglages. */
    fun nom(contexte: Context): String {
        val adresse = uri(contexte) ?: return contexte.getString(R.string.sonnerie_aucune)
        return try {
            RingtoneManager.getRingtone(contexte, adresse)?.getTitle(contexte)
                ?: contexte.getString(R.string.sonnerie_defaut)
        } catch (_: Exception) {
            contexte.getString(R.string.sonnerie_defaut)
        }
    }

    /**
     * Enregistre le choix. Un fichier ouvert via le sélecteur de documents doit
     * rester lisible après redémarrage, et l'interface système doit pouvoir le
     * lire elle-même pour le jouer au moment de la notification.
     */
    fun definir(contexte: Context, adresse: Uri?, persistable: Boolean) {
        if (adresse != null) {
            if (persistable) {
                try {
                    contexte.contentResolver.takePersistableUriPermission(
                        adresse,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: SecurityException) {
                    // Certaines sources ne proposent pas de permission persistante.
                }
            }
            try {
                contexte.grantUriPermission(
                    "com.android.systemui",
                    adresse,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
                // Sans effet sur les sonneries système, qui sont déjà lisibles.
            }
        }
        Reglages.definirSonnerie(contexte, adresse?.toString())
        Notifications.reconstruireCanaux(contexte)
    }

    /** Intention du sélecteur de sonneries du téléphone. */
    fun intentionSelecteur(contexte: Context): Intent =
        Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
            putExtra(
                RingtoneManager.EXTRA_RINGTONE_TITLE,
                contexte.getString(R.string.sonnerie_titre_selecteur)
            )
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            putExtra(
                RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI,
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            )
            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, uri(contexte))
        }

    /** Intention du sélecteur de fichiers, pour une sonnerie personnelle. */
    fun intentionFichier(): Intent =
        Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "audio/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

    /** Écoute au volume d'alarme, pour vérifier qu'on l'entendra vraiment. */
    fun ecouter(contexte: Context) {
        arreter()
        val adresse = uri(contexte) ?: return
        apercu = try {
            RingtoneManager.getRingtone(contexte, adresse)?.apply {
                audioAttributes = attributsAlarme
                play()
            }
        } catch (_: Exception) {
            null
        }
    }

    fun arreter() {
        try {
            apercu?.takeIf { it.isPlaying }?.stop()
        } catch (_: Exception) {
            // Rien à faire, l'aperçu s'arrête de toute façon.
        }
        apercu = null
    }

    fun enLecture(): Boolean = apercu?.isPlaying == true
}
