package fr.enzo.cachet45

import android.app.Activity
import android.os.Bundle
import android.widget.Toast

/**
 * Cible du raccourci « Démarrer » (appui long sur l'icône de l'appli).
 * Démarre le minuteur et se referme immédiatement, sans afficher d'écran.
 */
class DemarrageRapideActivity : Activity() {

    override fun onCreate(etatSauvegarde: Bundle?) {
        super.onCreate(etatSauvegarde)
        val message = when (Minuteur.etat(this)) {
            Etat.EN_COURS -> getString(
                R.string.deja_en_cours,
                MinuteurTile.formaterRestant(Minuteur.restantMs(this))
            )
            else -> {
                Minuteur.acquitter(this)
                Minuteur.demarrer(this)
                getString(R.string.minuteur_demarre, Reglages.dureeMin(this))
            }
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        finish()
    }
}
