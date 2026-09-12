package fr.enzo.cachet45

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Calendar

/** Consultation des prises, au choix en calendrier ou en liste. */
class HistoriqueActivity : Activity() {

    private lateinit var total: TextView
    private lateinit var ongletCalendrier: TextView
    private lateinit var ongletListe: TextView
    private lateinit var blocCalendrier: View
    private lateinit var blocListe: LinearLayout
    private lateinit var vide: TextView
    private lateinit var astuceListe: TextView
    private lateinit var moisLibelle: TextView
    private lateinit var calendrier: VueCalendrier
    private lateinit var resumeMois: TextView
    private lateinit var detailJour: TextView

    private var prises: List<Prise> = emptyList()
    private var calendrierAffiche = true
    private var annee = 0
    private var mois = 0
    private var jourSelectionne: Int? = null

    override fun onCreate(etatSauvegarde: Bundle?) {
        super.onCreate(etatSauvegarde)
        setContentView(R.layout.activity_historique)

        total = findViewById<TextView>(R.id.total)!!
        ongletCalendrier = findViewById<TextView>(R.id.onglet_calendrier)!!
        ongletListe = findViewById<TextView>(R.id.onglet_liste)!!
        blocCalendrier = findViewById<View>(R.id.bloc_calendrier)!!
        blocListe = findViewById<LinearLayout>(R.id.bloc_liste)!!
        vide = findViewById<TextView>(R.id.vide)!!
        astuceListe = findViewById<TextView>(R.id.astuce_liste)!!
        moisLibelle = findViewById<TextView>(R.id.mois_libelle)!!
        calendrier = findViewById<VueCalendrier>(R.id.calendrier)!!
        resumeMois = findViewById<TextView>(R.id.resume_mois)!!
        detailJour = findViewById<TextView>(R.id.detail_jour)!!

        val maintenant = Calendar.getInstance()
        annee = maintenant.get(Calendar.YEAR)
        mois = maintenant.get(Calendar.MONTH)
        jourSelectionne = maintenant.get(Calendar.DAY_OF_MONTH)

        ongletCalendrier.setOnClickListener { basculer(true) }
        ongletListe.setOnClickListener { basculer(false) }

        findViewById<View>(R.id.mois_precedent)!!.setOnClickListener { decalerMois(-1) }
        findViewById<View>(R.id.mois_suivant)!!.setOnClickListener { decalerMois(1) }

        calendrier.surJourChoisi = { jour ->
            jourSelectionne = jour
            majCalendrier()
        }
    }

    override fun onResume() {
        super.onResume()
        prises = Historique.toutes(this)
        majTout()
    }

    private fun basculer(versCalendrier: Boolean) {
        calendrierAffiche = versCalendrier
        majTout()
    }

    private fun decalerMois(pas: Int) {
        val curseur = Calendar.getInstance().apply {
            clear()
            set(annee, mois, 1)
            add(Calendar.MONTH, pas)
        }
        annee = curseur.get(Calendar.YEAR)
        mois = curseur.get(Calendar.MONTH)
        jourSelectionne = null
        majCalendrier()
    }

    private fun majTout() {
        total.text = resources.getQuantityString(
            R.plurals.prises_enregistrees,
            prises.size,
            prises.size
        )

        val aucune = prises.isEmpty()
        vide.visibility = if (aucune) View.VISIBLE else View.GONE
        blocCalendrier.visibility =
            if (!aucune && calendrierAffiche) View.VISIBLE else View.GONE
        blocListe.visibility =
            if (!aucune && !calendrierAffiche) View.VISIBLE else View.GONE
        astuceListe.visibility = blocListe.visibility

        styleOnglet(ongletCalendrier, calendrierAffiche)
        styleOnglet(ongletListe, !calendrierAffiche)

        if (aucune) return
        if (calendrierAffiche) majCalendrier() else majListe()
    }

    private fun styleOnglet(onglet: TextView, actif: Boolean) {
        onglet.setBackgroundResource(
            if (actif) R.drawable.puce_active else R.drawable.puce_inactive
        )
        onglet.setTextColor(getColor(if (actif) R.color.noir else R.color.gris))
    }

    // --- Calendrier ----------------------------------------------------------

    private fun majCalendrier() {
        val joursMarques = Historique.joursDuMois(prises, annee, mois)
        moisLibelle.text = Dates.moisEtAnnee(annee, mois)
        calendrier.afficher(annee, mois, joursMarques, jourSelectionne)

        resumeMois.text = resources.getQuantityString(
            R.plurals.prises_ce_mois,
            joursMarques.size,
            joursMarques.size
        )

        val jour = jourSelectionne
        if (jour == null) {
            detailJour.text = getString(R.string.choisir_un_jour)
            return
        }

        val duJour = Historique.duJour(prises, annee, mois, jour)
        detailJour.text = if (duJour.isEmpty()) {
            getString(R.string.aucune_prise_ce_jour, libelleDuJour(jour))
        } else {
            val lignes = duJour.joinToString("\n") { prise ->
                getString(
                    R.string.prise_heure_duree,
                    Dates.heure(this, prise.debut),
                    prise.dureeMin
                )
            }
            "${libelleDuJour(jour)}\n$lignes"
        }
    }

    private fun libelleDuJour(jour: Int): String {
        val date = Calendar.getInstance().apply {
            clear()
            set(annee, mois, jour)
        }
        return Dates.jourComplet(date.timeInMillis)
    }

    // --- Liste ---------------------------------------------------------------

    private fun majListe() {
        blocListe.removeAllViews()
        val densite = resources.displayMetrics.density
        var moisPrecedent = ""

        prises.forEach { prise ->
            val moisCourant = enTeteDeMois(prise.debut)
            if (moisCourant != moisPrecedent) {
                blocListe.addView(enTete(moisCourant, densite))
                moisPrecedent = moisCourant
            }
            blocListe.addView(ligne(prise, densite))
        }
    }

    private fun enTeteDeMois(horodatage: Long): String {
        val calendrier = Calendar.getInstance().apply { timeInMillis = horodatage }
        return Dates.moisEtAnnee(
            calendrier.get(Calendar.YEAR),
            calendrier.get(Calendar.MONTH)
        )
    }

    private fun enTete(libelle: String, densite: Float): TextView =
        TextView(this).apply {
            text = libelle
            textSize = 11f
            letterSpacing = 0.22f
            isAllCaps = true
            setTextColor(getColor(R.color.gris))
            val parametres = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            parametres.topMargin = (18 * densite).toInt()
            parametres.bottomMargin = (8 * densite).toInt()
            layoutParams = parametres
        }

    private fun ligne(prise: Prise, densite: Float): View {
        val rangee = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundResource(R.drawable.carte)
            setPadding(
                (22 * densite).toInt(),
                (15 * densite).toInt(),
                (22 * densite).toInt(),
                (15 * densite).toInt()
            )
            val parametres = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            parametres.bottomMargin = (8 * densite).toInt()
            layoutParams = parametres
            isLongClickable = true
            setOnLongClickListener {
                confirmerSuppression(prise)
                true
            }
        }

        val jour = TextView(this).apply {
            text = Dates.jourRelatif(this@HistoriqueActivity, prise.debut)
            textSize = 14f
            setTextColor(getColor(R.color.blanc))
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        val detail = TextView(this).apply {
            text = getString(
                R.string.prise_heure_duree,
                Dates.heure(this@HistoriqueActivity, prise.debut),
                prise.dureeMin
            )
            textSize = 13f
            setTextColor(getColor(R.color.gris))
        }

        rangee.addView(jour)
        rangee.addView(detail)
        return rangee
    }

    private fun confirmerSuppression(prise: Prise) {
        val quand = getString(
            R.string.prise_du,
            Dates.jourRelatif(this, prise.debut),
            Dates.heure(this, prise.debut)
        )
        AlertDialog.Builder(this)
            .setTitle(R.string.supprimer_prise)
            .setMessage(quand)
            .setNegativeButton(R.string.annuler, null)
            .setPositiveButton(R.string.supprimer) { _, _ ->
                Historique.supprimer(this, prise.debut)
                prises = Historique.toutes(this)
                majTout()
            }
            .show()
    }
}
