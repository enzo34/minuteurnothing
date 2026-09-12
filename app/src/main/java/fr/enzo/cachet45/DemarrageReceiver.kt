package fr.enzo.cachet45

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Les alarmes système sont effacées au redémarrage du téléphone et après une
 * mise à jour de l'appli : on les repose à partir de l'heure de fin enregistrée.
 */
class DemarrageReceiver : BroadcastReceiver() {

    override fun onReceive(contexte: Context, intention: Intent) {
        when (intention.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> Minuteur.restaurer(contexte)
        }
    }
}
