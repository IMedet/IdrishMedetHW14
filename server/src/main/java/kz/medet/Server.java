package kz.medet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
        list.forEach(clientHandler -> clientHandler.sendMessage(msg));
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
        return list.stream().anyMatch(clientHandler -> clientHandler.getUsername().equals(username));
    }

    public void sendPrivateMsg(ClientHandler sender, String receiver, String msg) {
        list.stream()
                .filter(clientHandler -> clientHandler.getUsername().equals(receiver))
                .findFirst()
                .ifPresentOrElse(
                        clientHandler -> {
                            clientHandler.sendMessage("From: " + sender.getUsername() + " Message: " + msg);
                            sender.sendMessage("Receiver: " + receiver + " Message: " + msg);
                            logger.info("Private message from " + sender.getUsername() + " to " + receiver + ": " + msg);
                        },
                        () -> sender.sendMessage("Unable to send message to " + receiver)
                );
    }

    public void sendClientList() {
        String clientList = list.stream()
                .map(ClientHandler::getUsername)
                .collect(Collectors.joining(" ", "/clients_list ",""));
    }

    public void updateUsername(String oldUsername, String newUsername) {
        list.stream()
                .filter(clientHandler -> clientHandler.getUsername().equals(oldUsername))
                .findFirst()
                .ifPresent(clientHandler -> clientHandler.sendMessage("Your username has been updated to " + newUsername));
    }

    public AuthenticationProvider getAuthenticationProvider() {
        return authenticationProvider;
    }
}
