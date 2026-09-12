#!/usr/bin/env bash
#
# Construit l'APK sans le SDK Android ni Gradle.
#
# Utile là où dl.google.com est inaccessible (et donc Google Maven, qui y
# redirige) : tous les outils viennent alors de Maven Central.
#   - aapt2 et les ressources du framework : embarqués dans apktool-lib
#   - dx (dexeur d'AOSP) : repackagé par Jake Wharton
#   - apksig : signature v2
#   - kotlinc : compilateur Kotlin embarquable
#   - android.jar : le android-all de Robolectric, qui contient tout le framework
#
# L'alignement de resources.arsc sur 4 octets, exigé par Android pour toute
# cible API 30+, est fait par outils/assembler_apk.py (zipalign réimplémenté).
#
# Usage : outils/construire_apk.sh [dossier_de_sortie]
# En temps normal, préférez simplement « ./gradlew assembleRelease ».

set -euo pipefail

RACINE="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SORTIE="${1:-$RACINE/build-manuel}"
CACHE="$SORTIE/outils"
TRAVAIL="$SORTIE/travail"
CENTRAL="https://repo.maven.apache.org/maven2"

APKTOOL=3.0.3
DX=16.0.1
APKSIG=2.3.0
KOTLIN=2.2.0
ROBOLECTRIC=15-robolectric-13954326

MIN_SDK=26
TARGET_SDK=35
VERSION_CODE=1
VERSION_NOM=1.0
PAQUET=fr.enzo.cachet45

mkdir -p "$CACHE" "$TRAVAIL/gen" "$TRAVAIL/classes"

recuperer() { # chemin_maven nom_local
  local destination="$CACHE/$2"
  [ -f "$destination" ] || curl -sSfL -o "$destination" "$CENTRAL/$1"
}

echo "== Récupération des outils =="
recuperer "org/apktool/apktool-lib/$APKTOOL/apktool-lib-$APKTOOL.jar" apktool.jar
recuperer "com/jakewharton/android/repackaged/dalvik-dx/$DX/dalvik-dx-$DX.jar" dx.jar
recuperer "com/android/tools/build/apksig/$APKSIG/apksig-$APKSIG.jar" apksig.jar
recuperer "org/robolectric/android-all/$ROBOLECTRIC/android-all-$ROBOLECTRIC.jar" android.jar
recuperer "org/jetbrains/kotlin/kotlin-compiler-embeddable/$KOTLIN/kotlin-compiler-embeddable-$KOTLIN.jar" kotlinc.jar
recuperer "org/jetbrains/kotlin/kotlin-stdlib/$KOTLIN/kotlin-stdlib-$KOTLIN.jar" kotlin-stdlib.jar
recuperer "org/jetbrains/kotlin/kotlin-reflect/$KOTLIN/kotlin-reflect-$KOTLIN.jar" kotlin-reflect.jar
recuperer "org/jetbrains/kotlin/kotlin-script-runtime/$KOTLIN/kotlin-script-runtime-$KOTLIN.jar" kotlin-script-runtime.jar
recuperer "org/jetbrains/kotlin/kotlin-daemon-embeddable/$KOTLIN/kotlin-daemon-embeddable-$KOTLIN.jar" kotlin-daemon.jar
recuperer "org/jetbrains/kotlinx/kotlinx-coroutines-core-jvm/1.8.1/kotlinx-coroutines-core-jvm-1.8.1.jar" coroutines.jar
recuperer "org/jetbrains/intellij/deps/trove4j/1.0.20200330/trove4j-1.0.20200330.jar" trove4j.jar
recuperer "org/jetbrains/annotations/13.0/annotations-13.0.jar" annotations.jar

if [ ! -x "$CACHE/aapt2" ]; then
  (cd "$CACHE" && unzip -o -q apktool.jar "prebuilt/linux/aapt2" "prebuilt/android-framework.jar" \
    && mv prebuilt/linux/aapt2 aapt2 && mv prebuilt/android-framework.jar framework.jar && rm -rf prebuilt)
  chmod +x "$CACHE/aapt2"
fi

echo "== Ressources =="
# aapt2 exige l'attribut package dans le manifeste, qu'AGP injecte d'habitude.
sed "s|<manifest |<manifest package=\"$PAQUET\" |" \
  "$RACINE/app/src/main/AndroidManifest.xml" > "$TRAVAIL/AndroidManifest.xml"

"$CACHE/aapt2" compile --dir "$RACINE/app/src/main/res" -o "$TRAVAIL/res.zip"
"$CACHE/aapt2" link -o "$TRAVAIL/base.apk" \
  -I "$CACHE/framework.jar" \
  --manifest "$TRAVAIL/AndroidManifest.xml" \
  -R "$TRAVAIL/res.zip" \
  --java "$TRAVAIL/gen" \
  --min-sdk-version "$MIN_SDK" --target-sdk-version "$TARGET_SDK" \
  --version-code "$VERSION_CODE" --version-name "$VERSION_NOM" \
  --auto-add-overlay

echo "== Code =="
javac -nowarn -source 8 -target 8 -d "$TRAVAIL/classes" \
  "$TRAVAIL/gen/${PAQUET//.//}/R.java"

# dx ne comprend pas invokedynamic : on force les lambdas et les conversions SAM
# à être compilées en classes anonymes, et la concaténation en StringBuilder.
CP_KOTLINC="$CACHE/kotlinc.jar:$CACHE/kotlin-stdlib.jar:$CACHE/kotlin-reflect.jar"
CP_KOTLINC="$CP_KOTLINC:$CACHE/kotlin-script-runtime.jar:$CACHE/kotlin-daemon.jar"
CP_KOTLINC="$CP_KOTLINC:$CACHE/coroutines.jar:$CACHE/trove4j.jar:$CACHE/annotations.jar"

java -cp "$CP_KOTLINC" org.jetbrains.kotlin.cli.jvm.K2JVMCompiler \
  -classpath "$CACHE/android.jar:$CACHE/kotlin-stdlib.jar:$TRAVAIL/classes" \
  -jvm-target 1.8 -Xlambdas=class -Xsam-conversions=class -Xstring-concat=inline \
  -nowarn -d "$TRAVAIL/classes" \
  "$RACINE"/app/src/main/java/${PAQUET//.//}/*.kt

echo "== Dex =="
# La stdlib Kotlin embarque un module-info Java 9 que dx ne sait pas lire.
rm -rf "$TRAVAIL/stdlib" && mkdir -p "$TRAVAIL/stdlib"
(cd "$TRAVAIL/stdlib" && unzip -q "$CACHE/kotlin-stdlib.jar" && rm -rf META-INF/versions \
  && find . -name "module-info.class" -delete)
(cd "$TRAVAIL" && jar cf stdlib.jar -C stdlib .)

java -cp "$CACHE/dx.jar" com.android.dx.command.Main --dex \
  --min-sdk-version="$MIN_SDK" --output="$TRAVAIL/classes.dex" \
  "$TRAVAIL/classes" "$TRAVAIL/stdlib.jar"

echo "== Assemblage et signature =="
python3 "$RACINE/outils/assembler_apk.py" \
  "$TRAVAIL/base.apk" "$TRAVAIL/classes.dex" "$TRAVAIL/nonsigne.apk"

javac -nowarn -cp "$CACHE/apksig.jar" -d "$TRAVAIL" "$RACINE/outils/Signeur.java"
# La signature v1 d'apksig 2.3.0 appelle une API interne du JDK disparue depuis
# Java 9 ; seule la v2 est produite, ce qui suffit largement au-dessus d'API 24.
java --add-exports java.base/sun.security.x509=ALL-UNNAMED \
  -cp "$CACHE/apksig.jar:$TRAVAIL" Signeur \
  "$TRAVAIL/nonsigne.apk" "$SORTIE/cachet45.apk" \
  "$RACINE/keystore/cachet45.jks" cachet45 cachet45 "$MIN_SDK"

echo "== Vérification =="
if python3 "$RACINE/outils/assembler_apk.py" verifier "$SORTIE/cachet45.apk" | grep -q "MAL ALIGNE"; then
  echo "Alignement incorrect, APK inutilisable." >&2
  exit 1
fi
"$CACHE/aapt2" dump badging "$SORTIE/cachet45.apk" | head -3

echo
echo "APK prêt : $SORTIE/cachet45.apk"
