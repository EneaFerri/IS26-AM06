# Architettura di Rete - Mesos

## Indice

1. [Panoramica](#1-panoramica)
2. [Porte e avvio del server](#2-porte-e-avvio-del-server)
3. [Socket e RMI](#3-socket-e-rmi)
4. [Interfacce principali](#4-interfacce-principali)
5. [Flusso dei callback](#5-flusso-dei-callback)
6. [Thread e concorrenza](#6-thread-e-concorrenza)
7. [Azioni client -> server](#7-azioni-client---server)
8. [Callback server -> client](#8-callback-server---client)
9. [Heartbeat e disconnessioni](#9-heartbeat-e-disconnessioni)
10. [Riconnessione](#10-riconnessione)
11. [Bot in caso di disconnessione](#11-bot-in-caso-di-disconnessione)
12. [Protocollo Socket](#12-protocollo-socket)
13. [Esempio di sessione completa](#13-esempio-di-sessione-completa)

---

## 1. Panoramica

Il progetto supporta due tecnologie di trasporto:

- Java RMI
- Socket TCP con messaggi JSON

L'idea e` far giocare client diversi nella stessa partita, anche se usano trasporti diversi. Un client RMI e un client Socket possono quindi condividere la stessa lobby e la stessa partita.

La logica di gioco resta separata dal trasporto grazie a due interfacce:

| Interfaccia | Direzione | Chi la implementa | Chi la usa |
|---|---|---|---|
| `GameServerProxy` | Client -> Server | `RmiClient`, `SocketServerProxy` | `CLIView`, `GUIView` |
| `VirtualView` | Server -> Client | `RmiClient`, `SocketClientHandler` | `GameController`, `LobbyManager` |

Questa struttura permette al resto del codice di non sapere se la connessione e` RMI o Socket.

---

## 2. Porte e avvio del server

| Servizio | Porta | Protocollo |
|---|---|---|
| RMI registry | 1099 | Java RMI |
| Socket TCP | 12345 | TCP / JSON |

Il punto di ingresso principale e` `CombinedServer`:

```text
CombinedServer.main()
├── crea un LobbyManager condiviso
├── registra lo shutdown hook per il DatabaseManager
├── avvia RmiServer sulla porta 1099
└── avvia SocketServer sulla porta 12345
```

Il `LobbyManager` e` condiviso tra i due server, cosi` i giocatori RMI e Socket possono stare nella stessa partita.

---

## 3. Socket e RMI

### Lato server

| Trasporto | Classe | Ruolo |
|---|---|---|
| RMI | `RmiServer` | Espone i metodi remoti e li inoltra al `LobbyManager` |
| Socket | `SocketServer` | Accetta le connessioni TCP e crea un handler per ogni client |
| Socket | `SocketClientHandler` | Legge i messaggi JSON e invia callback al client |

`SocketClientHandler` ha due responsabilita`:

- ricevere i messaggi dal client
- inviare i callback dal server al client

### Lato client

| Trasporto | Classe | Ruolo |
|---|---|---|
| RMI | `RmiClient` | Gestisce azioni verso il server e callback verso il client |
| Socket | `SocketClient` | Gestisce connessione, read loop, heartbeat e riconnessione dopo crash del server |
| Socket | `SocketServerProxy` | Serializza le azioni client -> server in JSON |

Con Socket la separazione tra `SocketClient` e `SocketServerProxy` e` utile per tenere distinti lettura e scrittura.

---

## 4. Interfacce principali

### `GameServerProxy`

Questa interfaccia rappresenta tutte le azioni che la view puo` inviare al server:

```text
GameServerProxy
├── loginFirstPlayer(nickname, numPlayers)
├── loginToLobby(nickname, lobbyId)
├── requestLobbyList()
├── placeTotem(nickname, letter)
├── pickCard(nickname, cardIndex, fromTop)
├── joinAsSpectator(nickname, lobbyId)
└── leaveSpectator(nickname)
```

### `VirtualView`

Questa interfaccia rappresenta tutti i callback che il server puo` inviare al client:

```text
VirtualView
├── onLoginAccepted(...)
├── onPlayerJoined(...)
├── onGameStarting(...)
├── onLobbyList(...)
├── onTurnSnapshot(...)
├── onYourTurn(...)
├── onTotemPlaced(...)
├── onInvalidAction(...)
├── onCardTaken(...)
├── onPlayerUpdated(...)
├── onTurnOrderUpdated(...)
├── onEventResolved(...)
├── onBoardUpdated(...)
├── onNewEraStarted(...)
├── onGameOver(...)
├── onRankingData(...)
├── onPlayerDisconnected(...)
├── onPlayerReplacedByBot(...)
├── onWaitingForServer(...)
├── onServerReconnected(...)
└── onSpectatorJoined(...)
```

### `VirtualServer<V>`

`VirtualServer<V>` descrive il contratto lato server.  
E` una versione generica dei metodi che il client puo` invocare, dove `V` rappresenta il tipo di view associata al client.

Nel caso RMI:

- `RmiServer` implementa `VirtualServer<VirtualViewRmi>`
- `VirtualServerRmi` estende questa interfaccia aggiungendo `Remote` e `throws RemoteException`

### `VirtualViewRmi`

`VirtualViewRmi` e` la versione RMI di `VirtualView`.  
Estende sia `Remote` sia `VirtualView`, e aggiunge `throws RemoteException` a tutti i metodi.

`RmiClient` implementa questa interfaccia ed e` l'oggetto remoto che il server usa per inviare i callback.

---

## 5. Flusso dei callback

I callback non arrivano direttamente alla view grafica o testuale. Il percorso e` questo:

```text
GameController / LobbyManager
  └── chiama VirtualView.onXxx(...)
        ├── [RMI] RmiClient.onXxx(...)
        │         └── ClientModel.onXxx(...)
        │               └── notifica tutti i ModelObserver registrati
        │                     └── CLIView / GUIView
        └── [Socket] SocketClientHandler.onXxx(...)
                  └── serializza JSON e lo invia sul socket
                        └── SocketClient.readLoop()
                              └── ClientModel.onXxx(...)
                                    └── notifica tutti i ModelObserver registrati
                                          └── CLIView / GUIView
```

`ClientModel` fa da ponte tra trasporto e interfaccia utente.  
`ModelObserver` e` l'interfaccia implementata da `CLIView` e `GUIView`.

---

## 6. Thread e concorrenza

### Server Socket

Per ogni client connesso vengono usati questi thread:

| Thread | Creato da | Scopo |
|---|---|---|
| `socket-client-<addr>` | `SocketServer` | Legge i messaggi in arrivo |
| `sender-<addr>` | `SocketClientHandler` | Invia i messaggi al client |
| `heartbeat` | `PingPongManager` | Gestisce il PING/PONG |
| `rmi-heartbeat-<nick>` | `RmiServer` | Verifica se il client RMI e` ancora attivo |

### Client Socket

| Thread | Creato da | Scopo |
|---|---|---|
| `socket-reader` | `SocketClient` | Legge i messaggi dal server |
| `heartbeat` | `PingPongManager` | Gestisce il PING/PONG |
| `socket-reconnect` | `SocketClient` | Prova a riconnettersi se il server non risponde |

### Client RMI

| Thread | Creato da | Scopo |
|---|---|---|
| Thread RMI | JVM | Gestisce le callback remote |
| `rmi-client-heartbeat` | `RmiClient` | Chiama `server.ping()` periodicamente |
| `rmi-reconnect` | `RmiClient` | Prova a ristabilire la connessione se il server non risponde |

### Perche` esiste un sender thread nel server Socket

La scrittura sul socket non viene fatta direttamente nel thread del controller.  
Questo evita che una scrittura lenta o bloccata possa rallentare la logica di gioco.

`SocketClientHandler` mette i messaggi in una coda e un thread separato li invia al client.  
In questo modo il controller non resta mai bloccato sulla rete.

---

## 7. Azioni client -> server

### Via RMI

```text
CLIView / GUIView
  └── GameServerProxy.placeTotem(nick, 'B')
        └── RmiClient.placeTotem(nick, 'B')
              └── server.placeTotem(nick, 'B')
                    └── RmiServer.placeTotem(...)
                          └── LobbyManager.placeTotem(...)
                                └── GameController.placeTotem(...)
```

Le chiamate RMI sono sincrone: il client aspetta il ritorno del metodo remoto.

### Via Socket

```text
CLIView / GUIView
  └── GameServerProxy.placeTotem(nick, 'B')
        └── SocketServerProxy.placeTotem(nick, 'B')
              └── invia un JSON newline-delimited sul socket
                    └── ritorno immediato
                          └── SocketClientHandler.dispatch(...)
                                └── LobbyManager.placeTotem(...)
                                      └── GameController.placeTotem(...)
```

Dal punto di vista del client, Socket funziona come invio di un messaggio senza aspettare una risposta immediata.  
La risposta arriva dopo, come callback separato.

---

## 8. Callback server -> client

### Via RMI

```text
GameController.broadcast(v -> v.onTotemPlaced(nick, spaceId))
  └── RmiClient.onTotemPlaced(nick, spaceId)
        └── ClientModel.onTotemPlaced(nick, spaceId)
              └── CLIView / GUIView
```

### Via Socket

```text
GameController.broadcast(v -> v.onTotemPlaced(nick, spaceId))
  └── SocketClientHandler.onTotemPlaced(nick, spaceId)
        └── enqueua il JSON nella sendQueue
              └── senderThread scrive sul socket
                    └── SocketClient.readLoop()
                          └── ClientModel.onTotemPlaced(...)
                                └── CLIView / GUIView
```

---

## 9. Heartbeat e disconnessioni

Il progetto usa un meccanismo di heartbeat basato su `PingPongManager`.

Ogni 10 secondi:

1. se il PING precedente non ha ricevuto risposta, viene considerato un timeout
2. altrimenti viene inviato un nuovo PING

Quando arriva un PONG, il flag interno viene resettato.  
Quando arriva un PING, il destinatario risponde subito con un PONG.

### Su Socket

Sia il server sia il client usano un `PingPongManager`.  
In questo modo entrambe le parti controllano la connessione.

### Su RMI

Su RMI la connessione viene gia` verificata da ogni chiamata remota, ma il progetto usa comunque un heartbeat esplicito:

- **Server -> Client**: `RmiServer` chiama `clientView.ping()`
- **Client -> Server**: `RmiClient` chiama `server.ping()`

Se una chiamata fallisce, il lato che la esegue considera la connessione persa.

---

## 10. Riconnessione

Questa e` la parte piu` importante da chiarire:

- **la riconnessione e` supportata solo dopo la caduta del server**
- **la riconnessione non serve a riprendersi una partita quando cade solo il client**

### Se cade il server

Quando il server si riavvia:

1. il `LobbyManager` ricarica da disco le partite salvate tramite `PersistenceManager`
2. i client ricevono `onWaitingForServer(...)`
3. quando il server torna disponibile, `onServerReconnected()` fa ripartire il login verso la partita ripristinata

In pratica, il client si ricollega al server riavviato e rientra nella partita salvata.

Questo comportamento e` presente sia in TUI sia in GUI:

- `CLIView.onServerReconnected()` richiama `server.loginFirstPlayer(myNick, savedNumPlayers)`
- `GUIView.onServerReconnected()` fa la stessa cosa su un thread separato

### Se cade solo il client

Se invece cade solo il client e il server continua a funzionare:

1. il server rileva la disconnessione
2. il giocatore viene gestito da `LobbyManager.handleDisconnect(...)`
3. se la partita e` in corso, il giocatore viene sostituito da un Bot

Quindi il client **non** ha una finestra di recupero per rientrare nella stessa partita gia` iniziata.  
Puo` eventualmente connettersi di nuovo al server come nuovo client, ma non riprende automaticamente il posto del giocatore sostituito dal Bot.

### Riconnessione di una partita salvata

Quando un client torna dopo il riavvio del server, `LobbyManager` cerca le partite in recovery:

- se trova una partita salvata che contiene quel nickname, richiama `reconnectPlayer(...)`
- al termine del recupero, la partita riprende dal punto in cui era stata interrotta

Questa logica vale per il recovery del **server**, non per la semplice caduta del **client**.

---

## 11. Bot in caso di disconnessione

`Bot` implementa `VirtualView` lato server.  
Quando un giocatore non si riconnette entro il tempo previsto, il server crea un `Bot` con lo stesso nickname e lo aggiunge alla lista delle view della partita.

Il bot riceve gli stessi callback di un client reale, ma prende decisioni automatiche invece di aspettare input umano.  
In questo modo la partita continua senza blocchi.

Per il `GameController` il bot e` trasparente: viene trattato come una normale `VirtualView`.

---

## 12. Protocollo Socket

Ogni messaggio TCP e` una riga JSON terminata da `\n`.

Esempio:

```json
{"type":"PLACE_TOTEM","payload":{"nickname":"Alice","letter":"B"}}
```

La classe `NetworkMessage` gestisce serializzazione e deserializzazione dei messaggi.

### Messaggi client -> server

| `MessageType` | Payload | Significato |
|---|---|---|
| `LOGIN_FIRST` | `nickname`, `numPlayers` | Crea una nuova lobby |
| `LOGIN_TO_LOBBY` | `nickname`, `lobbyId` | Entra in una lobby esistente |
| `REQUEST_LOBBY_LIST` | - | Richiede la lista delle lobby attive |
| `PLACE_TOTEM` | `nickname`, `letter` | Piazzamento del totem |
| `PICK_CARD` | `nickname`, `cardIndex`, `fromTop` | Scelta di una carta |
| `JOIN_AS_SPECTATOR` | `nickname`, `lobbyId` | Entra come spettatore |
| `LEAVE_SPECTATOR` | `nickname` | Esce dalla modalita` spettatore |
| `PING` / `PONG` | - | Heartbeat |

### Messaggi server -> client

| `MessageType` | Payload | Significato |
|---|---|---|
| `ON_LOGIN_ACCEPTED` | `nickname`, `expectedPlayers` | Login confermato |
| `ON_PLAYER_JOINED` | `nickname`, `currentCount`, `expected` | Un altro giocatore e` entrato |
| `ON_GAME_STARTING` | `playerNicknames` | La partita sta per iniziare |
| `ON_LOBBY_LIST` | `lobbies` | Lista lobby attive |
| `ON_ERROR` | `message` | Errore generico |
| `ON_NO_LOBBY_AVAILABLE` | - | Nessuna lobby disponibile |
| `ON_TURN_SNAPSHOT` | `currentPlayerNick`, `boardSummary` | Stato del turno ai giocatori in attesa |
| `ON_YOUR_TURN` | `nickname`, `phase`, `extraInfo` | Tocca a quel giocatore |
| `ON_TOTEM_PLACED` | `nickname`, `boardSpaceId` | Totem piazzato |
| `ON_INVALID_ACTION` | `nicknameTarget`, `errorMessage` | Azione non valida |
| `ON_CARD_TAKEN` | `nickname`, `cardId` | Carta presa |
| `ON_PLAYER_UPDATED` | `nickname` | Aggiornamento giocatore |
| `ON_TURN_ORDER_UPDATED` | `ordered` | Nuovo ordine di turno |
| `ON_EVENT_RESOLVED` | `eventName`, `details` | Evento risolto |
| `ON_BOARD_UPDATED` | - | Tabellone aggiornato |
| `ON_NEW_ERA_STARTED` | `era` | Nuova era iniziata |
| `ON_GAME_OVER` | `results` | Fine partita |
| `ON_RANKING_DATA` | `myRank`, `totalEntries`, `ranking` | Classifica dal database |
| `ON_PLAYER_DISCONNECTED` | `nickname` | Giocatore disconnesso |
| `ON_PLAYER_REPLACED_BY_BOT` | `nickname` | Giocatore sostituito da bot |
| `ON_SPECTATOR_JOINED` | `currentPlayerNick`, `boardSummary` | Snapshot iniziale per spettatore |
| `PING` / `PONG` | - | Heartbeat |

---

## 13. Esempio di sessione completa

```text
CLIENT (Alice)                            SERVER
  │                                          │
  │── loginFirstPlayer("Alice", 4) ─────────►│ LobbyManager.createLobby()
  │◄─ onLoginAccepted("Alice", 4) ───────────│ GameController.loginFirstPlayer()
  │                                          │
  │  [Bob, Carol e Dave si connettono]       │
  │◄─ onPlayerJoined("Bob", 2, 4) ───────────│
  │◄─ onPlayerJoined("Carol", 3, 4) ─────────│
  │◄─ onPlayerJoined("Dave", 4, 4) ──────────│
  │◄─ onGameStarting([Alice,Bob,Carol,Dave]) │ GameController.startGame()
  │                                          │
  │  Turno di Alice                          │
  │◄─ onTurnSnapshot("Alice", board) ────────│ broadcast a tutti
  │◄─ onYourTurn("Alice", TOTEM, info) ──────│ solo ad Alice
  │── placeTotem("Alice", 'B') ─────────────►│
  │◄─ onTotemPlaced("Alice", "spaceB") ──────│ broadcast a tutti
  │◄─ onYourTurn("Alice", CARD, info) ───────│ solo ad Alice
  │── pickCard("Alice", 2, true) ───────────►│
  │◄─ onCardTaken("Alice", "card_42") ───────│ broadcast a tutti
  │◄─ onPlayerUpdated("Alice") ──────────────│
  │◄─ onTurnOrderUpdated([...]) ─────────────│ fine turno
  │◄─ onEventResolved("Carestia", "...") ────│ fine round
  │◄─ onBoardUpdated() ──────────────────────│
  │                                          │
  │  [Dave si disconnette]                   │
  │◄─ onPlayerDisconnected("Dave") ──────────│
  │◄─ onPlayerReplacedByBot("Dave") ─────────│ dopo il timeout
  │                                          │
  │  Fine partita                            │
  │◄─ onGameOver("Alice:42,Bob:38,...") ─────│
  │◄─ onRankingData(1, 150, [...]) ──────────│ classifica dal DB
```

