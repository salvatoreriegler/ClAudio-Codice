# Installazione di ClAudio Codice

## Relay Linux

Installa Node.js 18+, npm, OpenSSL e curl. Copia la cartella `server/` del fork e avvia:

```bash
cd server
bash install-relay.sh
```

Lo script genera token e certificato, installa `ws`, avvia e verifica il relay. Stampa un link **claudio-codice://setup?...** e, se `qrencode` è installato, il QR. La porta iniziale è 8443: apri nel firewall quella configurata in `config.json`.

Copia il certificato pubblico `cert.pem` anche sul computer con Claude Code. La chiave privata `key.pem` resta sul server. Il protocollo del relay è compatibile con l'originale; lo schema del link è diverso per distinguere le app.

## Computer con Claude Code

Servono Python, `edge-tts` e `ffmpeg` nel PATH:

```bash
python -m pip install edge-tts
```

Prepara questa struttura nella cartella utente:

```text
~/.claude/voice/
  readback.py              ← desktop/readback.py
  ON                      ← file vuoto che abilita la lettura
  bridge/
    config.json           ← desktop/bridge-config.example.json, completato
    cert.pem              ← certificato pubblico del server
```

Esempio di `bridge/config.json`:

```json
{
  "url": "https://IL-TUO-SERVER:8443",
  "token": "IL-TUO-TOKEN",
  "cacert": "cert.pem",
  "voice": "it-IT-DiegoNeural"
}
```

Registra `readback.py` come hook **Stop** nelle impostazioni di Claude Code, usando i percorsi assoluti del tuo Python e dello script. Esempio da integrare in `~/.claude/settings.json`, preservando gli hook esistenti:

Riferimento: [documentazione ufficiale degli hook di Claude Code](https://code.claude.com/docs/en/hooks).

```json
{
  "hooks": {
    "Stop": [{
      "hooks": [{
        "type": "command",
        "command": "python /PERCORSO/ASSOLUTO/.claude/voice/readback.py"
      }]
    }]
  }
}
```

Su Windows, quota i percorsi con spazi e usa la corretta sintassi JSON per le barre inverse.
Copia `desktop/skill/voice-bridge/` in `~/.claude/skills/voice-bridge/` e invoca **/voice-bridge** in ogni sessione che vuoi usare. La skill eredita il monitor in background dell'upstream e descrive un'alternativa tramite Bash. La compatibilità va verificata con la versione di Claude Code installata.

Il nome della cartella di lavoro o il titolo personalizzato della sessione identifica il canale. Chiedi alla sessione di rispondere in italiano.

## Telefono

Installa l'APK di sviluppo **ClAudio Codice**. Scansiona il QR o apri il nuovo link; controlla il server nella conferma. In alternativa incolla `indirizzo|token|impronta` in **impostazioni → avanzate**.

Concedi il microfono quando richiesto. Attendi una risposta dalla sessione collegata, poi tocca il nome in **canali**. La **modalità a mani libere** predefinita consente invio dopo una pausa o dicendo «invia» con Android; il pulsante centrale funziona anche senza.

## Whisper facoltativo

La build `store` usa inizialmente Android in italiano. Per Whisper, compila `whisper.cpp` sul server Linux e scarica un modello multilingue, non una variante `.en`. Il servizio ereditato cerca:

```text
~/whisper.cpp/build/bin/whisper-cli
~/whisper.cpp/models/ggml-base.bin
~/voice_bridge/config.json
~/voice_bridge/utt/
```

Esegui il relay da `~/voice_bridge/`, copiandovi i file di `server/`, compreso **italian-commands.js**. Da quella cartella:

```bash
WHISPER_LANG=it COMMAND_LANG=it node transcribe.js
```

Puoi usare pm2 con le stesse variabili. `WHISPER_LANG` controlla la trascrizione, `COMMAND_LANG` i comandi. Entrambi sono italiani per impostazione predefinita; `WHISPER_LANG=auto` consente rilevamento automatico mantenendo i comandi italiani. Dopo l'avvio attiva **riconoscimento Whisper** nell'app.

## Build e controlli

Con JDK 17, Android SDK 35 e `ANDROID_HOME` configurato:

```powershell
.\gradlew.bat :app:testStoreDebugUnitTest :app:assembleStoreDebug
node --test server/test/*.test.js
```

Su Linux/macOS usa `./gradlew`. Il wrapper scarica Gradle e le dipendenze.
L'APK è `app/build/outputs/apk/store/debug/app-store-debug.apk`, firmato con chiave di sviluppo. Per una release stabile serve una chiave dedicata, secondo la configurazione `keystore.properties` del progetto.

Le verifiche automatiche non sostituiscono una prova con telefono, auricolari, relay e una sessione Claude Code reali.
