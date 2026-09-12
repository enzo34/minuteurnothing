package fr.enzo.cachet45

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

/** Affiche un texte en matrice de points, version écran (pas bitmap). */
class VueMatrice @JvmOverloads constructor(
    contexte: Context,
    attributs: AttributeSet? = null,
    style: Int = 0
) : View(contexte, attributs, style) {

    private val peinture = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }

    /** Distance entre deux centres de points, en dp. */
    var pasDp: Float = 9f
        set(valeur) {
            field = valeur
            requestLayout()
            invalidate()
        }

    /** Proportion du pas réellement remplie par le point. */
    var remplissage: Float = 0.72f
        set(valeur) {
            field = valeur
            invalidate()
        }

    var texte: String = ""
        set(valeur) {
            if (field == valeur) return
            field = valeur
            contentDescription = valeur
            requestLayout()
            invalidate()
        }

    var couleur: Int
        get() = peinture.color
        set(valeur) {
            peinture.color = valeur
            invalidate()
        }

    private val pasPx: Float
        get() = pasDp * resources.displayMetrics.density

    override fun onMeasure(largeurSpec: Int, hauteurSpec: Int) {
        val largeur = (Matrice.colonnes(texte) * pasPx).toInt() + paddingLeft + paddingRight
        val hauteur = (Matrice.LIGNES * pasPx).toInt() + paddingTop + paddingBottom
        setMeasuredDimension(
            resolveSize(largeur, largeurSpec),
            resolveSize(hauteur, hauteurSpec)
        )
    }

    override fun onDraw(canevas: Canvas) {
        super.onDraw(canevas)
        if (texte.isEmpty()) return
        val pas = pasPx
        val largeurTexte = Matrice.colonnes(texte) * pas
        val hauteurTexte = Matrice.LIGNES * pas
        Matrice.dessiner(
            canevas,
            texte,
            (width - largeurTexte) / 2f,
            (height - hauteurTexte) / 2f,
            pas,
            pas * remplissage,
            peinture
        )
    }
}
