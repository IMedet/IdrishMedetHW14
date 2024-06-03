package kz.medet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler implements Runnable{
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private Server server;
    public String username;


    public ClientHandler(Server server, Socket socket) throws IOException {
        this.server = server;
        this.socket = socket;
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());
    }


    @Override
    public void run() {
        try {
            login();
            readMessages();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }finally {
            disconnect();
        }
    }



    public void login() throws IOException {
        while (true) {
            String msg = in.readUTF();
            if (msg.startsWith("/login ")) {
                String checkName = msg.split("\\s+")[1];
                if (server.isUsernameTaken(checkName)) {
                    sendMessage("/login_failed Username is taken");
                }else {
                    username = checkName;
                    sendMessage("/login_ok " + username);
                    server.broadcastMessage(username + " has joined the chat");
                    server.subscribe(this);
                    break;
                }
            }
        }
    }

    public void readMessages() throws IOException {
        while (true){
            String msg = in.readUTF();
            if(msg.startsWith("/w ")){
                String[] arr = msg.split("\\s+",3);
                String toWhom = arr[1];
                String privateMessage = arr[2];
                server.sendPrivateMessage(username,toWhom,privateMessage);
                sendMessage(privateMessage);
            }else if(msg.equals("/who_am_i")){
                sendMessage("Your username is: " + username);
            }else {
                server.broadcastMessage(username + ": " + msg);
            }
        }
    }

    public void sendMessage(String msg) throws IOException {
        out.writeUTF(msg);
    }

    public void disconnect() {
        server.unsubscribe(this);
        if(socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public String getUsername() {
        return username;
    }
}
