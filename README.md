# ClAudio Codice

Parla in italiano con le tue sessioni di Claude Code dal telefono Android.
Ascolta le risposte, detta messaggi e passa da una conversazione all'altra.

Fork italiano di [Claudio Code di 1nspectorCat / VladiKo](https://github.com/1nspectorCat/Claudio-Code), distribuito con la stessa [licenza MIT](LICENSE).

## Edizione italiana

- Nome dell'app: **ClAudio Codice**.
- Schermata principale, canali, configurazione e avvisi vocali in italiano.
- Riconoscimento Android `it-IT`; Whisper impostato su `it`.
- Voce desktop italiana `it-IT-DiegoNeural`, modificabile nella configurazione.
- Cambio sessione a voce sia con Android sia con Whisper.
- Identificativo Android `it.claudiocodice.app` e link `claudio-codice://setup` dedicati: può essere installata accanto all'originale.

Questa prima edizione traduce il percorso principale. I registri tecnici e le schermate sperimentali dell'hub conservano parte dei testi inglesi; commenti e documentazione interna ereditati possono essere in russo. Il server dell'hub multi-provider non è incluso nell'upstream: questa versione collega le sessioni di **Claude Code**.

## Comandi vocali

Pronuncia il comando come frase autonoma. Con il riconoscitore Android, attiva **modalità a mani libere** per inviare a voce.

| Comando | Effetto |
| --- | --- |
| «Elenca le sessioni» | Legge le quattro sessioni più recenti con il loro numero. |
| «Passa alla seconda» | Seleziona la seconda sessione dell'elenco. |
| «Passa alla seconda sessione» | Stessa selezione. |
| «Passa a SECURDOC 1» / «Passa a SECURDOC uno» | Seleziona la chat per nome, anche con prefissi Windows/WSL. |
| «Vai a Collaudo 1» | Stesso cambio per nome. |
| «Passa alla sessione contabilità» | Cerca il canale per nome. Ascolta la conferma del destinatario. |
| «Torna in automatico» | Rimuove scelta esplicita e filtro dei canali. |
| «Invia» / «Invia per favore» | Invia la bozza dettata. |
| «Annulla» | Cancella la bozza. |
| «Fine comunicazione» | Cancella la bozza e mette il microfono in attesa. |

Le parole dentro una frase normale, come «il programma invia i dati», restano testo.
Per cambiare canale a voce, invia o annulla prima la bozza. La numerazione segue l'ultimo elenco letto per tre minuti e non è un identificativo permanente.
Per i nomi non serve pronunciare prefissi tecnici o identificativi. Se più chat corrispondono al nome, l'app mantiene il destinatario corrente e legge l'elenco.

## Collegamenti

```text
Claude Code sul computer ←→ relay sul tuo server ←→ ClAudio Codice su Android
                            code per sessione
```

Servono Android 8+, Claude Code sul computer e un server Linux con Node.js 18+. Whisper è facoltativo. La voce italiana non traduce le risposte: chiedi a Claude Code di rispondere in italiano.

- [Installazione e compilazione](docs/SETUP.md)
- [Guida all'uso](docs/GUIDE.md)
- [Flusso dei dati](docs/PRIVACY.md)
- [Origine e stato del fork](docs/FORK.md)

## Verifiche

```bash
node --test server/test/*.test.js
./gradlew :app:testStoreDebugUnitTest :app:assembleStoreDebug
```

Su Windows usa `gradlew.bat`. Servono JDK 17 e Android SDK 35.
L'APK `storeDebug` è una build di sviluppo, firmata con la chiave di debug locale.

Copyright originale: **2026 VladiKo**. `LICENSE` è preservato integralmente.
ClAudio Codice è un fork indipendente, non un prodotto ufficiale di Anthropic.
