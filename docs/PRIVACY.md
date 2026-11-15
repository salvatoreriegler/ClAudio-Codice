# Dove passano i dati

ClAudio Codice usa un relay configurato e gestito dall'utente. Questo documento descrive il flusso tecnico dei dati del fork.

- **Riconoscitore Android**: l'audio viene gestito dal servizio installato sul telefono. L'elaborazione può usare servizi online, secondo provider e configurazione.
- **Whisper**: i segmenti audio viaggiano tramite TLS verso il relay scelto e vengono trascritti da `whisper.cpp` sul server.
- **Risposte vocali desktop**: `readback.py` usa `edge-tts`, che invia il testo al servizio vocale Microsoft. Il sistema quindi non è interamente locale. L'audio prodotto passa dal relay al telefono.
- **Avvisi Android**: sono sintetizzati dal motore TTS del dispositivo; elaborazione locale o remota dipende da quel motore.
- **Messaggi a Claude Code**: passano dal relay alla sessione e al provider AI configurato.

L'app conserva configurazione e cronologia localmente, con backup Android disattivato. Il relay conserva code, testo e audio: parte dello stato è in memoria, mentre i canali gestiti possono usare code persistenti.

Il progetto non include analytics, pubblicità o un relay condiviso gestito dal fork. I registri possono contenere testo, nomi dei canali e indirizzi: controllali prima di condividerli. Token, chiavi TLS e impostazioni personali sono esclusi dal repository.
