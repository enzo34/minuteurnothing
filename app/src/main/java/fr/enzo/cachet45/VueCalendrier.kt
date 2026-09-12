package fr.enzo.cachet45

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import java.util.Calendar

/**
 * Un mois sous forme de grille : un rond par jour, plein si le cachet a été pris.
 * Dans la continuité de la matrice de points du reste de l'appli.
 */
class VueCalendrier @JvmOverloads constructor(
    contexte: Context,
    attributs: AttributeSet? = null,
    styleParDefaut: Int = 0
) : View(contexte, attributs, styleParDefaut) {

    private val densite = resources.displayMetrics.density
    private val hauteurEntete = 30f * densite

    private val fond = Paint(Paint.ANTI_ALIAS_FLAG)
    private val contour = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * densite
    }
    private val texte = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = 12f * densite
    }
    private val texteEntete = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = 10f * densite
        letterSpacing = 0.12f
    }

    private val couleurBlanc = contexte.getColor(R.color.blanc)
    private val couleurNoir = contexte.getColor(R.color.noir)
    private val couleurGris = contexte.getColor(R.color.gris)
    private val couleurContour = contexte.getColor(R.color.contour)
    private val couleurRouge = contexte.getColor(R.color.rouge)

    private val initiales = listOf("L", "M", "M", "J", "V", "S", "D")

    var annee: Int = 2026
        private set
    var mois: Int = 0
        private set

    private var joursMarques: Set<Int> = emptySet()
    private var jourSelectionne: Int? = null

    /** Appelé quand l'utilisateur touche un jour du mois. */
    var surJourChoisi: ((Int) -> Unit)? = null

    fun afficher(annee: Int, mois: Int, joursMarques: Set<Int>, jourSelectionne: Int?) {
        this.annee = annee
        this.mois = mois
        this.joursMarques = joursMarques
        this.jourSelectionne = jourSelectionne
        requestLayout()
        invalidate()
    }

    private fun premierDuMois(): Calendar = Calendar.getInstance().apply {
        clear()
        set(annee, mois, 1)
    }

    /** Nombre de cases vides avant le 1er, la semaine commençant le lundi. */
    private fun decalage(): Int =
        (premierDuMois().get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7

    private fun nombreDeJours(): Int =
        premierDuMois().getActualMaximum(Calendar.DAY_OF_MONTH)

    private fun nombreDeSemaines(): Int {
        val total = decalage() + nombreDeJours()
        return (total + 6) / 7
    }

    override fun onMeasure(largeurSpec: Int, hauteurSpec: Int) {
        val largeur = resolveSize(suggestedMinimumWidth, largeurSpec)
        val cellule = largeur / 7f
        val hauteur = (hauteurEntete + nombreDeSemaines() * cellule).toInt()
        setMeasuredDimension(largeur, resolveSize(hauteur, hauteurSpec))
    }

    override fun onDraw(canevas: Canvas) {
        super.onDraw(canevas)
        val cellule = width / 7f
        val rayon = cellule * 0.34f

        texteEntete.color = couleurGris
        initiales.forEachIndexed { colonne, initiale ->
            canevas.drawText(
                initiale,
                colonne * cellule + cellule / 2f,
                hauteurEntete * 0.62f,
                texteEntete
            )
        }

        val aujourdhui = Calendar.getInstance()
        val memeMois = aujourdhui.get(Calendar.YEAR) == annee &&
            aujourdhui.get(Calendar.MONTH) == mois
        val jourActuel = if (memeMois) aujourdhui.get(Calendar.DAY_OF_MONTH) else -1

        val debut = decalage()
        for (jour in 1..nombreDeJours()) {
            val index = debut + jour - 1
            val centreX = (index % 7) * cellule + cellule / 2f
            val centreY = hauteurEntete + (index / 7) * cellule + cellule / 2f
            val pris = jour in joursMarques

            if (pris) {
                fond.color = couleurBlanc
                canevas.drawCircle(centreX, centreY, rayon, fond)
            } else {
                contour.color = couleurContour
                canevas.drawCircle(centreX, centreY, rayon, contour)
            }

            if (jour == jourActuel) {
                contour.color = couleurRouge
                canevas.drawCircle(centreX, centreY, rayon + 3f * densite, contour)
            }

            if (jour == jourSelectionne) {
                contour.color = couleurBlanc
                canevas.drawCircle(centreX, centreY, rayon + 6f * densite, contour)
            }

            texte.color = if (pris) couleurNoir else couleurGris
            val metriques = texte.fontMetrics
            val base = centreY - (metriques.ascent + metriques.descent) / 2f
            canevas.drawText(jour.toString(), centreX, base, texte)
        }
    }

    override fun onTouchEvent(evenement: MotionEvent): Boolean {
        when (evenement.actionMasked) {
            MotionEvent.ACTION_DOWN -> return true
            MotionEvent.ACTION_UP -> {
                val cellule = width / 7f
                if (evenement.y < hauteurEntete) return true
                val colonne = (evenement.x / cellule).toInt().coerceIn(0, 6)
                val ligne = ((evenement.y - hauteurEntete) / cellule).toInt()
                val jour = ligne * 7 + colonne - decalage() + 1
                if (jour in 1..nombreDeJours()) {
                    performClick()
                    surJourChoisi?.invoke(jour)
                }
                return true
            }
        }
        return super.onTouchEvent(evenement)
    }

    override fun performClick(): Boolean = super.performClick()
}
