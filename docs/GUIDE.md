## Aggiornamento 1.48-it.6

Le risposte partono senza ripetere ogni volta il nome del canale. La modifica si applica anche installando l’aggiornamento sopra una versione precedente. Il nome resta visibile e il cambio chat conserva la sua conferma vocale. Se vuoi gli annunci, attiva **Impostazioni → annuncia il canale prima della risposta**; la tua scelta successiva viene mantenuta.

Per la sola sintesi vocale, `SECURDOC` viene scritto `Securdoc`, così il maiuscolo non suggerisce una lettura come acronimo. Il nome sullo schermo non cambia; la resa finale dipende dalla voce Android.

## Aggiornamento 1.48-it.5

**Nomi sempre aggiornati.** Il nome della chat viene applicato a tutte le etichette della cronologia, anche ai messaggi precedenti. L’app controlla i nomi ogni cinque secondi mentre è aperta. Non serve riaprire Canali. Il contenuto dei messaggi non viene riscritto.

**Cambio manuale.** Tocca il grande nome della chat in alto oppure **Canali**. Il selettore occupa tutta la schermata: ogni riga è un unico bersaglio grande e la chat attuale è evidenziata. Tocca una riga per scegliere destinatario e conversazione visualizzata.

**Alias locali.** Nel selettore tocca **Alias locali**, poi la sessione; oppure tieni premuto il suo nome. Scrivi un alias e premi **Salva**. Il nome viene usato nell’app, negli annunci e nei comandi come «passa a Documenti». Anche il nome originale resta riconoscibile. Un alias già usato da un’altra sessione viene rifiutato; se un successivo cambio sul PC rende un nome ambiguo, l’app chiede di scegliere invece di cambiare destinatario arbitrariamente. **Usa nome originale** ripristina il nome del PC. Gli alias restano sul telefono e sono legati a quella specifica sessione, non a tutte le chat della stessa cartella.

**Interrompere con la voce.** Con **Claude · via PC**, durante la risposta attendi l’indicazione **Puoi interrompere parlando** e comincia a parlare. L’app interrompe la lettura e continua la stessa registrazione, conservando le prime parole. «Aspetta, controlla invece…» è un altro modo per farlo. Non occorre premere invio: dopo la pausa consueta parte il messaggio. Il testo della risposta interrotta resta nella cronologia. La funzione ferma la lettura sul telefono, non annulla un lavoro già avviato sul PC.

**Controlli.** Il microfono resta attivo durante le risposte per questa funzione. Il pulsante microfono spento disattiva anche l’ascolto durante la riproduzione. Puoi disabilitare **Interrompi parlando** nelle impostazioni avanzate. Questa modalità richiede il riconoscimento Claude; Android e Whisper restano disponibili con l’interruzione manuale. La cancellazione dell’eco dipende dal dispositivo e dagli auricolari: prova la funzione da fermo prima di usarla in mobilità.

## Aggiornamento 1.48-it.4

Con il relay configurato per Claude, il riconoscimento passa automaticamente a **Claude · via PC**. Attendi **In ascolto** prima di parlare. La bozza si aggiorna mentre parli; dopo circa 6 secondi di pausa l’app completa la trascrizione e invia. Una connessione lenta può aggiungere qualche secondo. Resta possibile dire **«passa a SECURDOC 1»** come frase autonoma.

Se il servizio vocale si interrompe, l’app conserva la bozza e mostra il motivo senza inviarne una versione incompleta. **Impostazioni → avanzate → Riconoscimento Claude** permette di tornare al riconoscimento Android. Non serve avviare `/voice` sul PC né un modello sulla GPU.

## Aggiornamento 1.48-it.3

- **Canali**: tocca il nome per scegliere sia la chat mostrata sia il destinatario della voce. Le righe grandi sostituiscono i vecchi comandi separati «attiva» e «rispondi».
- **Mani libere**: attiva per impostazione predefinita, anche al primo aggiornamento da it.2. La scelta successiva viene rispettata. Avvia il microfono da fermo; detta e fai una pausa di circa 6 secondi. L’ascolto riprende dopo invio e risposta. «Fine comunicazione» interrompe l’ascolto.
- Guarda la **bozza dal vivo** per controllare il riconoscimento. Un errore di microfono, rete o destinatario compare sulla schermata principale.
- Con relay e hook it.3, la risposta scritta arriva prima dell’audio, anche se il servizio di sintesi vocale non è disponibile.

# Guida a ClAudio Codice

## Primo messaggio

1. Configura il relay sul telefono. Se sul PC sono installati gli hook di integrazione nativa, le chat compaiono automaticamente; nell’installazione upstream manuale avvia `/voice-bridge` nella sessione Claude Code.
2. Chiedi alla sessione una risposta in italiano: il canale apparirà nell'app.
3. Apri **canali** e tocca il nome della chat desiderata.
4. Detta il messaggio e fai una pausa di circa 6 secondi. Puoi anche pronunciare **invia** come frase autonoma.

Con Claude o Android, «invia» deve essere riconosciuto come frase separata: fai una pausa fra il testo e il comando. **Invia dopo una pausa** abilita invece l'invio automatico.

## Conversazioni e destinatario

- Toccare una riga seleziona quella chat. **Ascolta tutti i canali** azzera eventuali filtri di ascolto precedenti.
- Toccare una riga seleziona sia la chat da leggere sia il destinatario della voce.
- La chat scelta resta il destinatario anche se arriva la risposta di un altro canale. «Torna in automatico» ripristina la risposta all’ultima sessione ascoltata.
- Fra risposte di due canali hai 12 secondi per rispondere al primo. Se inizi a parlare, la coda aspetta.

Per cambiare a voce, termina prima la bozza e pronuncia «elenca le sessioni», poi «passa alla seconda sessione». La numerazione segue l'ultimo elenco letto per tre minuti; senza elenco recente usa le quattro sessioni più recenti.

Puoi dire direttamente **«passa a SECURDOC 1»**, **«passa a SECURDOC uno»** oppure **«vai a Collaudo 1»**. Restano valide le forme «passa al canale SECURDOC 1» e «passa alla sessione contabilità». Non occorre pronunciare i prefissi Windows/WSL o il codice fra parentesi quadre.

Il nome completo ha precedenza: «RARE Dashboard» e «RARE Dashboard fork» indicano chat diverse. Se il nome è ambiguo, il destinatario rimane invariato e l'app legge l'elenco: scegli per numero o usa **canali → nome della chat**. Ascolta sempre la conferma del destinatario. **Torna in automatico** azzera destinatario esplicito e filtro di ascolto.

Con Claude o Android, una richiesta di navigazione durante una bozza rimane testo. Con Whisper, la navigazione deve occupare l'intera dettatura. Invia o annulla prima di cambiare canale.

## Pulsante centrale e annullamento

- Da fermo: attiva il microfono.
- Durante la dettatura: invia il messaggio.
- Durante una risposta: mette in pausa o riprende.
- Pressione prolungata: accende o spegne il servizio.
- **Interrompi e parla** interrompe la lettura per dettare.

**Annulla** cancella la bozza. Pronuncialo come frase autonoma; Whisper accetta anche una frase finale separata «Annulla» dopo il testo. **Fine comunicazione**, pronunciato da solo, cancella la bozza e mette il microfono in attesa.

## Impostazioni

**Segui automaticamente gli auricolari** segue il loro collegamento: senza auricolari l'audio può uscire dall'altoparlante. **Solo auricolari**, con modalità automatica disattivata, lascia le risposte in coda quando gli auricolari mancano.

**Modalità a mani libere** abilita il microfono continuo e l'invio vocale con Claude o Android. Attiva **riconoscimento Whisper** solo dopo aver configurato il servizio sul server.

Se un canale mostra **non riceve**, verifica che la sessione sia aperta e che la sua integrazione nativa sia attiva; nell’installazione manuale upstream riattiva `/voice-bridge`. La sezione **diagnostica** mostra collegamento ed eventi; parte dei messaggi tecnici è ancora in inglese.
