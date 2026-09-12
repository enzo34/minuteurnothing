# Cachet 45

Minuteur d'un seul appui pour le traitement de l'hypothyroïdie (Hashimoto) :
on prend le cachet le matin, on appuie sur le widget, et l'application prévient
quand le délai de jeûne est écoulé et qu'on peut enfin manger et boire.

Pensé pour Nothing OS (CMF Phone 2 Pro), dans le style maison : pilules
anthracite, gros chiffres en matrice de points, petites capitales espacées.

![Aperçu du widget et de l'application](apercu.svg)

## Ce que ça fait

- **Un appui sur le widget** = le minuteur démarre. Rien d'autre à faire.
- Le widget affiche les **minutes restantes** et **l'heure à laquelle vous pourrez manger**.
- Une notification silencieuse décompte dans la barre d'état.
- À la fin : **sonnerie + vibration**, « C'est bon, vous pouvez manger ».
- **Sonnerie au choix** : une alarme du téléphone ou n'importe quel fichier
  audio, avec un bouton pour l'écouter avant de la retenir.
- **Journal des prises**, consultable en **calendrier** (un rond plein par jour
  où le cachet a été pris) ou en **liste** groupée par mois.
- Délai réglable : 30, 45 ou 60 minutes (45 par défaut).
- Le minuteur survit au redémarrage du téléphone et à la veille profonde.

### Trois façons de lancer le minuteur, toutes en un geste

| Où | Comment |
| --- | --- |
| Widget de l'écran d'accueil | un appui sur la pilule |
| Réglages rapides | volet déroulant → tuile « Cachet 45 » |
| Icône de l'appli | appui long → « Démarrer » |

Pendant le décompte, un appui sur le widget ouvre l'appli, d'où l'on peut
annuler. Le minuteur ne peut donc pas être remis à zéro par erreur.

## Installer sur le téléphone

1. Ouvrir, **depuis le téléphone**, la page
   [Releases](../../releases/tag/derniere-version) du dépôt.
2. Télécharger `cachet45.apk`.
3. Ouvrir le fichier téléchargé et autoriser l'installation depuis cette source.
4. Au premier lancement, accepter les notifications, puis appuyer sur
   **« Ajouter le widget à l'écran d'accueil »**.

L'APK est reconstruit automatiquement à chaque modification du code
(onglet **Actions** du dépôt).

> **Si un job échoue en deux secondes, sans aucun journal :** aucune machine ne
> lui a été attribuée, la compilation n'a donc jamais commencé. La raison exacte
> s'affiche en bandeau rouge en haut de la page du run. Les deux causes
> habituelles sont un dépôt privé (les minutes y sont facturées) — corrigé en
> passant le dépôt en public — et un problème de facturation sur le compte, à
> régler sur <https://github.com/settings/billing>. En attendant, la compilation
> locale ci-dessous ne dépend de rien de tout cela.

### Sans passer par GitHub

Ouvrir le dossier dans **Android Studio** puis lancer *Run* sur le téléphone
branché en USB (débogage USB activé). Android Studio télécharge seul le SDK
nécessaire.

Et si même le SDK Android est hors d'atteinte (réseau filtré, machine sans
Android Studio) :

```bash
outils/construire_apk.sh     # → build-manuel/cachet45.apk
```

Ce script reconstruit l'APK sans SDK ni Gradle, en assemblant à la main une
chaîne d'outils entièrement récupérée depuis Maven Central : `aapt2` et les
ressources du framework extraits d'apktool, le dexeur `dx` d'AOSP, `apksig`
pour la signature v2, et le `android.jar` complet publié par Robolectric.
Il ne lui faut qu'un JDK 17, Python 3 et `curl`.

## Fonctionnement interne

Volontairement sans service en arrière-plan ni thread : le décompte est
entièrement porté par l'horloge système.

- `Minuteur` — démarrage, annulation, fin. L'échéance est posée avec
  `AlarmManager.setAlarmClock()`, la seule variante qu'Android ne repousse
  jamais, même en veille profonde ou en économie d'énergie.
- `Reglages` — l'état du minuteur tient dans quelques valeurs
  (`SharedPreferences`) : début, fin, durée choisie, sonnerie. L'état courant se
  déduit de l'heure de fin comparée à l'heure actuelle, donc rien ne peut se
  désynchroniser.
- `Historique` — le journal des prises, en JSON dans les mêmes préférences. Une
  prise par jour pendant dix ans tient dans moins de 200 Ko : une base de
  données serait disproportionnée. Annuler dans les deux premières minutes
  efface la prise (fausse manœuvre) ; annuler plus tard la conserve, puisque le
  cachet, lui, a bien été pris.
- `Sonneries` — Android fige le son d'un canal de notification à sa création :
  en changer impose d'en recréer un neuf, d'où le numéro de version dans son
  identifiant et le ménage des canaux devenus inutiles.
- `Matrice` / `VueMatrice` — les chiffres en points. La police NDot de Nothing
  n'étant pas redistribuable, chaque glyphe est dessiné sur une grille de 5×7
  vrais cercles, réutilisée pour le widget (bitmap), l'écran principal (canvas)
  et l'icône de l'appli (vecteur généré).
- `MinuteurWidget` — le widget affiche des minutes, il est donc redessiné une
  fois par minute seulement, via une alarme qui se replanifie toute seule.
- `DemarrageReceiver` — repose l'alarme après un redémarrage du téléphone, une
  mise à jour de l'appli ou un changement d'heure.

## Compiler soi-même

```bash
./gradlew assembleRelease   # app/build/outputs/apk/release/app-release.apk
```

Il faut un SDK Android (API 35) et un JDK 17.

## Avertissement

Cette application est un minuteur, pas un dispositif médical. Le délai à
respecter entre la prise du cachet et le premier repas est celui que vous a
indiqué votre médecin ou votre pharmacien.
