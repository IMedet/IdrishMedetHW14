package kz.medet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Collections;
import java.util.List;

public class ClientHandler {
    private static final Logger logger = LogManager.getLogger(ClientHandler.class);

    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private Server server;
    private String username;

    public ClientHandler(Server server, Socket socket) throws IOException {
        this.server = server;
        this.socket = socket;
        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());

        new Thread(() -> {
            try {
                while (true) {
                    String msg = in.readUTF();
                    if (msg.startsWith("/login ")) {
                        // /login Bob@gmail.com 111
                        String[] tokens = msg.split("\\s+");
                        if (tokens.length != 3) {
                            sendMessage("Server: Incorrect command");
                            continue;
                        }
                        String login = tokens[1];
                        String password = tokens[2];
                        String nick = server.getAuthenticationProvider().getUsernameByLoginAndPassword(login, password);
                        if (nick == null) {
                            sendMessage("/login_failed Incorrect login/password");
                            logger.warn("Login failed for login: " + login);
                            continue;
                        }

                        if (server.isUserOnline(nick)) {
                            sendMessage("/login_failed this username is already in use");
                            logger.warn("Login failed for " + nick + ": Username already in use");
                            continue;
                        }
                        username = nick;
                        sendMessage("/login_ok " + username);
                        server.subscribe(this);
                        logger.info(username + " logged in");
                        break;
                    }
                }
                while (true) {
                    String msg = in.readUTF();
                    // /p Bob Hello
                    if (msg.startsWith("/")) {
                        executeCmd(msg);
                        continue;
                    }
                    server.broadcastMessage(username + ": " + msg);
                    logger.info("Broadcast message from " + username + ": " + msg);
                }
            } catch (IOException e) {
                logger.error("Connection error with client " + username, e);
            } finally {
                disconnect();
            }
        }).start();
    }

    public void executeCmd(String msg) throws IOException {
        if (msg.startsWith("/p ")) {
            String[] tokens = msg.split("\\s+", 3);
            server.sendPrivateMsg(this, tokens[1], tokens[2]);
            return;
        }

        if (msg.startsWith("/change_nick ")) {
            String[] tokens = msg.split("\\s+", 3);
            String newUsername = tokens[1];
            if (!server.isUserOnline(newUsername)) {
                String oldUsername = this.username;
                this.username = newUsername;
                sendMessage("You have changed your username to: " + newUsername);
                server.updateUsername(oldUsername, newUsername);
                server.sendClientList();
            } else {
                sendMessage("Username " + newUsername + " is already in use.");
            }
            return;
        }

        if (msg.equals("/who_am_i")) {
            sendMessage("Your username is: " + username);
            logger.info(username + " requested their nickname");
            return;
        }
    }

    public void sendMessage(String msg) {
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            disconnect();
        }
    }

    public void disconnect() {
        server.unsubscribe(this);
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        logger.info(username + " disconnected");
    }

    public String getUsername() {
        return username;
    }
}