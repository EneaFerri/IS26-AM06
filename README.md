# Mesos

Progetto di Ingegneria del Software per la realizzazione software del gioco da tavolo **Mesos**.

L'obiettivo del progetto é stato sviluppare una versione completa e giocabile del titolo, rispettando le regole del manuale e i vincoli architetturali richiesti dalla consegna: architettura client-server, pattern MVC, comunicazione tramite **RMI** e **Socket**, interfaccia **TUI/GUI**, e funzionalitá avanzate come multi-lobby, persistenza e classifica su database.

## Contenuti

- [Panoramica](#panoramica)
- [Scelte implementative](#scelte-implementative)
- [GUI: approccio e tecniche](#gui-approccio-e-tecniche)
- [Pattern e responsabilita`](#pattern-e-responsabilita)
- [Funzionalita` implementate](#funzionalita-implementate)
- [Avvio del progetto](#avvio-del-progetto)
- [Documentazione aggiuntiva](#documentazione-aggiuntiva)

## Panoramica

Il progetto é strutturato come applicazione distribuita con un unico server condiviso e piú client. I client possono connettersi sia via **RMI** sia via **Socket TCP**, anche nella stessa partita.

Il gioco supporta:

- gestione delle lobby;
- partite complete con regole ufficiali;
- interfaccia testuale e interfaccia grafica;
- salvataggio dello stato su disco;
- classifica persistente su database;
- gestione delle disconnessioni con recupero della partita;
- sostituzione automatica dei giocatori disconnessi con bot server-side.

## Scelte implementative

### Architettura generale

Abbiamo adottato una separazione netta tra:

- **model**, che contiene le regole del gioco e lo stato della partita;
- **controller**, che valida le azioni dei client e orchestra il flusso di gioco;
- **view**, che si occupa della presentazione lato TUI e GUI;
- **network**, che astrae il trasporto RMI/Socket;
- **persistence** e **database**, che gestiscono rispettivamente il salvataggio su disco e la classifica storica.

L'entry point del server é [`CombinedServer`](src/main/java/it/polimi/ingsw/CombinedServer.java), che avvia sia il server RMI sia il server Socket usando un unico [`LobbyManager`](src/main/java/it/polimi/ingsw/controller/LobbyManager.java) condiviso.

### Gestione del modello di gioco

La logica di gioco é concentrata nel package "model":

- [`Game`](src/main/java/it/polimi/ingsw/model/Game.java) mantiene stato, turni, fasi, mazzi e board;
- `model/cards` implementa carte tribú, edifici, eventi e personaggi;
- `model/board` gestisce la configurazione degli spazi offerta e dell'ordine di turno;
- `model/player` rappresenta il giocatore e il totem.

La configurazione di carte e board é stata resa **data-driven**: diversi elementi sono caricati da file JSON in `src/main/resources`, cosí da ridurre la logica hardcoded e mantenere separati dati e comportamento.

### Gestione delle lobby

Il server supporta piú lobby simultanee. Il [`LobbyManager`](src/main/java/it/polimi/ingsw/controller/LobbyManager.java):

- crea nuove lobby;
- permette di unirsi a una lobby esistente;
- espone la lista delle lobby attive;
- inoltra le azioni al controller corretto;
- gestisce il recupero delle partite salvate su disco.

### Comunicazione di rete

Per la rete abbiamo usato un'interfaccia comune, [`GameServerProxy`](src/main/java/it/polimi/ingsw/network/GameServerProxy.java), cosí la view non dipende dal trasporto effettivo.

Sul lato server, le callback verso i client passano attraverso [`VirtualView`](src/main/java/it/polimi/ingsw/VirtualView.java). Le implementazioni concrete sono:

- `RmiClient` per il trasporto RMI;
- `SocketClientHandler` per il trasporto Socket;
- `Bot` per i giocatori sostituiti automaticamente in caso di disconnessione.

In questo modo il controller puo` inviare notifiche senza conoscere i dettagli della connessione.

## GUI: approccio e tecniche

La GUI é realizzata con **JavaFX** e costruita direttamente in codice, anche nella creazione dei nodi, senza FXML.

L'implementazione é fortemente **imperativa** nella gestione di:

- creazione delle scene;
- cambio schermata;
- aggiornamento di stato;
- animazioni;
- overlay modali;
- effetti grafici;
- logica di interazione con il server.

Questa scelta ci ha dato un controllo molto preciso sul comportamento della schermata di gioco e ci ha permesso di creare una UI piú dinamica e personalizzata.

### Tecniche usate nella GUI

- costruzione manuale delle scene in `GUIView` e `GameScreen`;
- aggiornamenti UI su thread dedicato tramite `Platform.runLater`;
- animazioni con `AnimationTimer`, `FadeTransition` e transizioni dedicate;
- overlay per dettagli carta, regolamento e ranking;
- effetti grafici come blur, glass panels e sfondi animati;
- gestione separata della schermata di lobby, attesa, partita e fine partita;
- caricamento di immagini, audio e regole visuali direttamente dalle risorse.

La GUI non si limita a “mostrare” il gioco, ma cerca di accompagnare il turno con feedback visivi chiari, mantenendo leggibile la partita anche quando la board si aggiorna.

## Pattern e responsabilita`

### MVC

Il pattern usato é **Model-View-Controller**:

- la `View` riceve input dell'utente;
- il `Controller` valida e traduce le azioni;
- il `Model` applica le regole del gioco e aggiorna lo stato.

### Observer

Abbiamo usato il pattern **Observer** in due punti distinti:
1. **Server side**
   - [`Game`](src/main/java/it/polimi/ingsw/model/Game.java) notifica [`GameObserver`](src/main/java/it/polimi/ingsw/model/GameObserver.java);
   - [`GameController`](src/main/java/it/polimi/ingsw/controller/GameController.java) riceve gli eventi del modello e li traduce in callback verso i client.

2. **Client side**
   - [`ClientModel`](src/main/java/it/polimi/ingsw/view/ClientModel.java) riceve le callback di rete;
   - notifica gli osservatori [`ModelObserver`](src/main/java/it/polimi/ingsw/view/ModelObserver.java), ovvero `CLIView` e `GUIView`.

Questa doppia catena di osservatori é stata molto utile per mantenere separati trasporto, stato locale e presentazione.

### Altri pattern e tecniche

- **Singleton** per `DatabaseManager` e `PersistenceManager`;
- **Adapter** per uniformare RMI e Socket dietro le stesse interfacce;
- **Snapshot / DTO** per serializzare il salvataggio della partita;
- **Executor / thread dedicati** per heartbeat, reconnect e bot;
- **composition over inheritance** dove possibile, soprattutto nella view.

## Funzionalita` implementate

### Funzionalita` principali

- partita completa con regole ufficiali di Mesos;
- gestione di tutti i tipi di carte e delle relative interazioni;
- supporto al turno e alla progressione delle ere;
- interfaccia testuale (TUI);
- interfaccia grafica (GUI) in JavaFX;
- selezione della UI all'avvio del client;
- selezione del trasporto all'avvio del client;
- lobby creation/joining con nickname univoco.

### Funzionalita` avanzate

- **Multi lobby** sullo stesso server;
- **persistenza su disco e ripristino** dello stato di gioco;
- **bot automatici** che sostituiscono i giocatori disconnessi;
- **classifica su database** con storico delle partite;

## Avvio del progetto

### Requisiti

- Java 21
- Maven
- MySQL 8+
- JavaFX 21

### Server

Compilazione:

```bash
mvn clean package
```

Avvio del server:

```bash
java -jar target/server.jar
```

Il server espone:

- **RMI** su porta `1099`
- **Socket** su porta `12345`

### Client

Avvio del client TUI o GUI:

```bash
java -jar target/client.jar
```

All'avvio il client permette di scegliere:

- interfaccia `TUI` o `GUI`;
- trasporto `RMI` o `Socket`;
- indirizzo del server;
- nickname del giocatore.

In alternativa, per la GUI si puó usare il launcher JavaFX configurato nel `pom.xml`.

### Database ranking

Prima di avviare il server, configurare il database tramite:

- [`schema.sql`](schema.sql)
- [`src/main/resources/db/db.properties`](src/main/resources/db/db.properties)

Per maggiori dettagli rimando al file Implementazione DB info all'interno del progetto.

## Documentazione aggiuntiva

- Documentazione di rete: [`README_NETWORK.md`](README_NETWORK.md)
- Schema database: [`schema.sql`](schema.sql)
- Risorse grafiche e audio: `src/main/resources`
