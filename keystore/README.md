# Clé de signature

`cachet45.jks` signe l'APK. Mot de passe du magasin et de la clé : `cachet45`,
alias `cachet45`.

Elle est volontairement versionnée en clair, et ce n'est **pas** un secret : son
seul rôle est que chaque nouvelle version s'installe par-dessus la précédente
sans avoir à désinstaller l'appli (Android refuse une mise à jour signée par une
autre clé). Si la construction générait une clé neuve à chaque fois, chaque mise
à jour obligerait à désinstaller — et donc à perdre l'historique.

Conséquence à connaître : n'importe qui peut signer un APK avec cette clé. Tant
que l'application reste personnelle et s'installe depuis ce dépôt, c'est sans
conséquence. En revanche, avant toute publication sur un magasin d'applications,
il faut générer une clé neuve, la garder privée (secret GitHub), et ne plus la
versionner :

```bash
keytool -genkeypair -v -keystore cachet45.jks -storetype PKCS12 \
  -alias cachet45 -keyalg RSA -keysize 2048 -validity 10950
```
