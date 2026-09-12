package fr.enzo.cachet45

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Point d'entrée unique des actions : widget, notifications, alarme de fin. */
class MinuteurReceiver : BroadcastReceiver() {

    override fun onReceive(contexte: Context, intention: Intent) {
        when (intention.action) {
            Minuteur.ACTION_DEMARRER -> Minuteur.demarrer(contexte)
            Minuteur.ACTION_ANNULER -> Minuteur.annuler(contexte)
            Minuteur.ACTION_FIN -> Minuteur.terminer(contexte)
            Minuteur.ACTION_ACQUITTER -> Minuteur.acquitter(contexte)
            Minuteur.ACTION_TIC -> Minuteur.tic(contexte)
        }
    }
}
