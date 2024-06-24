package kz.medet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Server {
    private static final Logger logger = LogManager.getLogger(Server.class);

    private int port;
    private List<ClientHandler> list;
    private AuthenticationProvider authenticationProvider;

    public Server(int port) {
        this.port = port;
        this.list = new ArrayList<>();
        this.authenticationProvider = new InMemoryAuthProvider();
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            logger.info("Server started on port " + port);
            System.out.println("Сервер запущен на порту 8189. Ожидаем подключение клиента...");
            while (true) {
                Socket socket = serverSocket.accept();
                logger.info("Client connected:" + socket.getInetAddress());
                new ClientHandler(this, socket);
            }
        } catch (IOException e) {
            logger.error("Error starting server: ", e);
        }
    }

    public void broadcastMessage(String msg) {
        for (ClientHandler clientHandler : list) {
            clientHandler.sendMessage(msg);
        }
    }

    public void subscribe(ClientHandler clientHandler) {
        list.add(clientHandler);
        sendClientList();
        logger.info(clientHandler.getUsername() + " joined the chat");
    }

    public void unsubscribe(ClientHandler clientHandler) {
        list.remove(clientHandler);
        sendClientList();
        logger.info(clientHandler.getUsername() + " left the chat");
    }

    public boolean isUserOnline(String username) {
        for (ClientHandler clientHandler : list) {
            if (clientHandler.getUsername().equals(username)) {
                return true;
            }
        }
        return false;
    }

    public void sendPrivateMsg(ClientHandler sender, String receiver, String msg) {
        for (ClientHandler c : list) {
            if (c.getUsername().equals(receiver)) {
                c.sendMessage("From: " + sender.getUsername() + " Message: " + msg);
                sender.sendMessage("Receiver: " + receiver + " Message: " + msg);
                logger.info("Private message from " + sender.getUsername() + " to " + receiver + ": " + msg);
                return;
            }
        }
        sender.sendMessage("Unable to send message to " + receiver);
    }

    public void sendClientList() {
        StringBuilder builder = new StringBuilder("/clients_list ");
        for (ClientHandler c : list) {
            builder.append(c.getUsername()).append(" ");
        }
        builder.setLength(builder.length() - 1);
        String clientList = builder.toString();
        for (ClientHandler c : list) {
            c.sendMessage(clientList);
        }
    }

    public void updateUsername(String oldUsername, String newUsername) {
        for (ClientHandler clientHandler : list) {
            if (clientHandler.getUsername().equals(oldUsername)) {
                clientHandler.sendMessage("Your username has been updated to " + newUsername);
                break;
            }
        }
    }

    public AuthenticationProvider getAuthenticationProvider() {
        return authenticationProvider;
    }
}
