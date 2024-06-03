package kz.medet;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Server {
    private int port;
    private List<ClientHandler> list;
    public Server(int port) {
        this.port = port;
        this.list = new ArrayList<>();
        try(ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Сервер запущен на порту 8189. Ожидаем подключение клиента...");
            while (true) {
                Socket socket = serverSocket.accept();
                new Thread(new ClientHandler(this, socket)).start();
            }

        } catch (IOException e){
            e.printStackTrace();
        }
    }

    public void broadcastMessage(String msg) throws IOException {
        for(ClientHandler clientHandler : list) {
            clientHandler.sendMessage(msg);
        }
    }

    public void sendPrivateMessage(String fromUsername, String toUsername, String msg) throws IOException {
        for (ClientHandler client : list){
            if(client.getUsername().equals(toUsername)){
                client.sendMessage("Message from " + fromUsername + ": " + msg);
                break;
            }
        }
    }

    public void subscribe(ClientHandler clientHandler) {
        list.add(clientHandler);
    }

    public void unsubscribe(ClientHandler clientHandler) {
        list.remove(clientHandler);
    }

    public boolean isUsernameTaken(String username){
        for (ClientHandler client : list){
            if(client.getUsername().equals(username)){
                return true;
            }
        }

        return false;
    }
}
