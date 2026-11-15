# Origine e stato del fork

- Nome: **ClAudio Codice**.
- Originale: https://github.com/1nspectorCat/Claudio-Code
- Commit di partenza: `7de4ec62841859234e12c6529e94b7f9e9d57b99`.
- Versione: `1.48-it.1`, versionCode 149.
- Licenza MIT, attribuzione originale a VladiKo preservata.

Il fork è pubblicato su [salvatoreriegler/ClAudio-Codice](https://github.com/salvatoreriegler/ClAudio-Codice), nel ramo `main`. Il remote `origin` punta al fork, mentre `upstream` rimane il riferimento al progetto originale. Il branch locale `italiano` conserva la preparazione iniziale.

Le traduzioni italiane sono il terzo argomento di `tr(ru, en, it)`. I testi originali restano per agevolare i merge. Se manca la traduzione, viene usato l'inglese. Il percorso principale è tradotto; l'hub non pubblicato e alcuni messaggi tecnici restano in inglese.

`ItalianCommands.kt` e `server/italian-commands.js` implementano la navigazione in entrambe le modalità di riconoscimento. I test leggono lo stesso corpus di frasi positive e negative e verificano anche cambio destinatario e gestione della bozza nel servizio Android.

Prima di una release stabile servono una prova completa su telefono e relay, una chiave di firma dedicata e le traduzioni delle funzioni sperimentali eventualmente abilitate.
