package kz.medet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;


public class ServerApp {


    public static void main( String[] args ) {
        try(ServerSocket serverSocket = new ServerSocket(8189)) {
            System.out.println("Сервер запущен на порту 8189. Ожидаем подключение клиента...");
            Socket socket = serverSocket.accept();
            DataInputStream in = new DataInputStream(socket.getInputStream());
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            System.out.println("Клиент подключился");

            int messageAmount = 0;
            String msg;
            while (true) {
                msg = in.readUTF();
                if(msg.equalsIgnoreCase("/stat")){
                    out.writeUTF("Количество сообщений - " + messageAmount);
                }else {
                    messageAmount++;
                    System.out.print(msg + "\n");
                    out.writeUTF("ECHO: " + msg);
                }
            }
        } catch (IOException e){
            e.printStackTrace();
        }

    }
}
