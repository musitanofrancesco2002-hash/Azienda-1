# Azienda — AndroidIDE compatibile

Questa versione è stata resa più compatibile con AndroidIDE evitando il plugin DSL che
può causare l'errore "Cannot choose between variants" durante la sincronizzazione.

## Versioni usate
- Android Gradle Plugin: 8.5.2
- Gradle: 8.7
- Kotlin: 1.9.24
- Compose compiler: 1.5.14
- JDK: 17

## In AndroidIDE
1. Estrai lo ZIP.
2. Apri la cartella `azienda` (deve contenere `gradlew`, `settings.gradle.kts`, `build.gradle.kts` e `app`).
3. Lascia terminare la sincronizzazione.
4. Se la sincronizzazione è OK, esegui `assembleDebug` oppure:
   `./gradlew apk`
5. L'APK viene copiato nella cartella principale con nome `Azienda-debug.apk`.

Se AndroidIDE continua a usare un Gradle/JDK precedente, aggiorna gli strumenti con
`idesetup -c` e verifica con `java --version` che sia JDK 17.
