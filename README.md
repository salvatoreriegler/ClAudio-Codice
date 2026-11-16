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

## Conversazione dal telefono (1.48-it.3)

Configura e avvia l’app da fermo. Apri **Canali** e tocca il nome della chat: voce, testo e conversazione visualizzata hanno lo stesso destinatario. Puoi poi cambiare dicendo «Passa a SECURDOC 1». Il destinatario resta selezionato finché non lo cambi.

Le parole riconosciute appaiono subito nella bozza a schermo. Dopo l’invio, il messaggio resta nella conversazione; gli errori sono visibili. Se manca una chat, la bozza viene conservata. Le righe dei canali sono grandi e mostrano il nome senza prefissi tecnici.

Con relay e hook aggiornati, il testo della risposta arriva prima della sintesi vocale. L’audio parte automaticamente; il riconoscimento si sospende durante la riproduzione per non ritrascriverla. **Mani libere** è disattivabile dalla schermata principale. Il riconoscimento continuo usa microfono e batteria finché non lo metti in pausa o interrompi la comunicazione.

## Comandi vocali

Pronuncia il comando come frase autonoma. La modalità **Mani libere** è attiva per impostazione predefinita: dopo aver avviato il microfono, fai una pausa di circa 6 secondi per inviare. Il microfono riprende automaticamente dopo invio e risposta; «Fine comunicazione» lo ferma.

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
cd server && npm ci && npm test
cd ..
python desktop/test_readback.py
./gradlew :app:testStoreDebugUnitTest :app:assembleStoreDebug
```

Su Windows usa `gradlew.bat`. Servono JDK 17 e Android SDK 35.
L'APK `storeDebug` è una build di sviluppo, firmata con la chiave di debug locale.

Copyright originale: **2026 VladiKo**. `LICENSE` è preservato integralmente.
ClAudio Codice è un fork indipendente, non un prodotto ufficiale di Anthropic.
