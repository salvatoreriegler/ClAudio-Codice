# ClAudio Codice

Parla in italiano con le tue sessioni di Claude Code dal telefono Android.
Ascolta le risposte, detta messaggi e passa da una conversazione all'altra.

Fork italiano di [Claudio Code di 1nspectorCat / VladiKo](https://github.com/1nspectorCat/Claudio-Code), distribuito con la stessa [licenza MIT](LICENSE).

## Edizione italiana

- Nome dell'app: **ClAudio Codice**.
- Schermata principale, canali, configurazione e avvisi vocali in italiano.
- Riconoscimento Claude in streaming tramite il PC, in italiano; Android e Whisper restano disponibili.
- Voce desktop italiana `it-IT-DiegoNeural`, modificabile nella configurazione.
- Cambio sessione a voce con Claude, Android e Whisper.
- Identificativo Android `it.claudiocodice.app` e link `claudio-codice://setup` dedicati: può essere installata accanto all'originale.

Questa prima edizione traduce il percorso principale. I registri tecnici e le schermate sperimentali dell'hub conservano parte dei testi inglesi; commenti e documentazione interna ereditati possono essere in russo. Il server dell'hub multi-provider non è incluso nell'upstream: questa versione collega le sessioni di **Claude Code**.

## Nomi, alias e interruzione vocale (1.48-it.5)

- Il nome corrente della sessione viene mostrato anche sui messaggi già salvati. L’elenco si aggiorna ogni cinque secondi mentre l’app è aperta, senza aprire Canali; i vecchi eventi audio non sovrascrivono il nome verificato.
- Tocca il grande selettore in alto oppure **Canali**: si apre una schermata dedicata con righe da almeno 104 dp, nomi grandi e chat selezionata evidenziata.
- **Canali → Alias locali → chat** permette di scegliere un nome da usare su questo telefono. Puoi anche tenere premuto il nome della chat. L’alias appare nella cronologia e funziona con «passa a Documenti», per esempio. **Usa nome originale** lo rimuove. Il nome sul PC resta invariato; l’alias è associato all’ID della sessione e non viene trasferito a fork o chat nuove.
- Con riconoscimento Claude e **Interrompi parlando** attivo, puoi parlare durante la lettura della risposta. Quando la voce viene riconosciuta, la lettura si ferma e le prime parole restano nella bozza. Puoi anche dire «aspetta» o «stop» e continuare. La risposta interrotta resta nella cronologia.

Durante questa modalità il microfono resta attivo anche mentre l’app legge. L’app usa cancellazione dell’eco Android, uscita audio di comunicazione e confronto con il testo letto. Il controllo **microfono spento** interrompe anche questo ascolto. **Interrompi parlando**, nelle impostazioni avanzate, è disattivabile; le modalità Android/Whisper mantengono il comportamento precedente. La reattività dipende da rete, microfono e auricolari: i test automatici non sostituiscono la verifica sul telefono.

## Riconoscimento Claude (1.48-it.4)

Il relay può usare il servizio di dettatura di Claude Code con l’accesso Claude già presente sul PC. L’audio viene trasmesso ad Anthropic; le credenziali restano sul PC. Le parole provvisorie appaiono a schermo e vengono corrette prima dell’invio. I nomi delle chat disponibili vengono passati come suggerimenti al riconoscitore. Non serve una GPU locale.

È un’integrazione **non ufficiale**, basata sul protocollo del client Claude Code, che Anthropic può modificare. Esistono precedenti pubblici: [prova di concetto originale](https://gist.github.com/byt3bl33d3r/061dad31daa37b1d81dfa8cc67d45168) e [integrazione in Handy](https://github.com/cjpais/Handy/discussions/1033). La [documentazione ufficiale della dettatura](https://code.claude.com/docs/en/voice-dictation) descrive requisiti e trattamento dell’audio.

Vedi [configurazione del riconoscimento](docs/SETUP.md#riconoscimento-claude-tramite-il-pc). L’app mostra **Claude · via PC** quando il relay lo abilita. In caso di errore conserva la bozza e ferma l’invio; non passa silenziosamente ad Android. Per scegliere Android, disattiva **Riconoscimento Claude** nelle impostazioni avanzate. Su relay privi della funzionalità resta il comportamento precedente.

## Conversazione dal telefono (1.48-it.3)

Configura e avvia l’app da fermo. Apri **Canali** e tocca il nome della chat: voce, testo e conversazione visualizzata hanno lo stesso destinatario. Puoi poi cambiare dicendo «Passa a SECURDOC 1». Il destinatario resta selezionato finché non lo cambi.

Le parole riconosciute appaiono subito nella bozza a schermo. Dopo l’invio, il messaggio resta nella conversazione; gli errori sono visibili. Se manca una chat, la bozza viene conservata. Le righe dei canali sono grandi e mostrano il nome senza prefissi tecnici.

Con relay e hook aggiornati, il testo della risposta arriva prima della sintesi vocale. L’audio parte automaticamente. Con Claude e **Interrompi parlando**, l’app ascolta anche durante la risposta; altrimenti sospende il riconoscimento durante la riproduzione. **Mani libere** è disattivabile dalla schermata principale. Il riconoscimento continuo usa microfono e batteria finché non lo metti in pausa o interrompi la comunicazione.

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
