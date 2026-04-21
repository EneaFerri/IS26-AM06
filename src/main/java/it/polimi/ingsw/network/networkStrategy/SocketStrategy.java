package it.polimi.ingsw.network.networkStrategy;

import it.polimi.ingsw.network.message.Message;
import it.polimi.ingsw.network.message.MessageType;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class SocketStrategy implements NetworkStrategy {

    private Socket               socket;
    private ObjectOutputStream   out;
    private ObjectInputStream    in;
    private NetworkListener listener;
    private String               nickname;
    private volatile boolean     connected = false;

    private final ExecutorService readerExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "socket-reader");
        t.setDaemon(true);
        return t;
    });

    // ── NetworkStrategy ───────────────────────────────────────────────

    @Override
    public void setListener(NetworkListener listener) {
        this.listener = listener;
    }

    @Override
    public void connect(String host, int port) throws IOException {
        socket    = new Socket(host, port);
        out       = new ObjectOutputStream(socket.getOutputStream());
        out.flush();                                   // send stream header immediately
        in        = new ObjectInputStream(socket.getInputStream());
        connected = true;
        readerExecutor.submit(this::readerLoop);
    }

    @Override
    public void login(String nickname) {
        this.nickname = nickname;
        send(new Message(MessageType.LOGIN_REQUEST, nickname, nickname));
    }


    @Override
    public void drawCard(int cardId) {
        send(new Message(MessageType.DRAW_CARD, nickname, cardId));
    }

    @Override
    public void placeTotem(char letter) {
        send(new Message(MessageType.PLACE_TOTEM, nickname, letter));
    }



    @Override
    public void disconnect() {
        connected = false;
        readerExecutor.shutdownNow();
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
    }

    // ── Internal ──────────────────────────────────────────────────────

    /**
     * Serialises and sends a message. Synchronised on {@code out} to
     * prevent concurrent writes from interleaving on the stream.
     */
    private synchronized void send(Message message) {
        if (!connected) return;
        try {
            out.writeObject(message);
            out.flush();
            out.reset();   // clear serialisation cache so mutable objects are re-serialised
        } catch (IOException e) {
            handleDisconnection();
        }
    }

    /**
     * Runs on the reader thread until the connection is closed.
     * Handles {@code PING} transparently; forwards everything else to the listener.
     */
    private void readerLoop() {
        try {
            while (connected && !Thread.currentThread().isInterrupted()) {
                Message msg = (Message) in.readObject();
                if (msg.getType() == MessageType.PING) {
                    send(new Message(MessageType.PONG, nickname != null ? nickname : "UNKNOWN"));
                    continue;
                }
                if (listener != null) listener.onMessage(msg);
            }
        } catch (IOException | ClassNotFoundException e) {
            handleDisconnection();
        }
    }

    private void handleDisconnection() {
        if (!connected) return;
        connected = false;
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
        if (listener != null) listener.onDisconnected();
    }
}