package kz.medet.client;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.nio.file.*;
import java.util.ResourceBundle;
import java.util.stream.Stream;

public class Controller implements Initializable {

    @FXML
    private TextField msgField, loginField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private TextArea msgArea;
    @FXML
    private HBox loginBox, msgBox;
    @FXML
    ListView<String> clientsList;

    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private String username;
    private Path historyFile;

    public void setUsername(String username) {
        this.username = username;
        if (this.username == null) {
            loginBox.setVisible(true);
            loginBox.setManaged(true);
            msgBox.setVisible(false);
            msgBox.setManaged(false);
            clientsList.setVisible(false);
            clientsList.setManaged(false);
        } else {
            loginBox.setVisible(false);
            loginBox.setManaged(false);
            msgBox.setVisible(true);
            msgBox.setManaged(true);
            clientsList.setVisible(true);
            clientsList.setManaged(true);
            historyFile = Paths.get(username + "_history.txt");
            loadHistory();
        }
    }

    public void connect() {
        try {
            socket = new Socket("localhost", 8189);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            new Thread(() -> {
                try {
                    // цикл авторизации
                    while (true) {
                        String msg = in.readUTF();
                        if (msg.startsWith("/login_ok ")) {
                            setUsername(msg.split("\\s+")[1]);
                            historyFile = Paths.get(username + "_history.txt");
                            break;
                        }
                        if (msg.startsWith("/login_failed ")) {
                            String reason = msg.split("\\s+", 2)[1];
                            msgArea.appendText(reason + "\n");
                        }
                    }
                    // цикл общения
                    while (true) {
                        String msg = in.readUTF();
                        if (msg.startsWith("/clients_list ")) {
                            Platform.runLater(() -> {
                                clientsList.getItems().clear();
                                String[] tokens = msg.split("\\s+");
                                for (int i = 1; i < tokens.length; i++) {
                                    clientsList.getItems().add(tokens[i]);
                                }
                            });
                            continue;
                        }
                        msgArea.appendText(msg + "\n");
                        writeHistory(msg);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    disconnect();
                }
            }).start();

        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Unable to connect to server");
        }
    }

    public void login() {
        if (socket == null || socket.isClosed()) {
            connect();
        }

        if (loginField.getText().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Имя пользователя не может быть пустым", ButtonType.OK);
            alert.showAndWait();
            return;
        }
        try {
            out.writeUTF("/login " + loginField.getText() + " " + passwordField.getText());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        setUsername(null);
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendMsg() {
        try {
            out.writeUTF(msgField.getText());
            msgField.clear();
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Невозможно отправить сообщение");
            alert.showAndWait();
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setUsername(null);
    }

    private void writeHistory(String msg) {
        try {
            Files.write(historyFile, (msg + System.lineSeparator()).getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadHistory() {
        if (historyFile != null && Files.exists(historyFile)) {
            try (Stream<String> lines = Files.lines(historyFile)) {
                lines.forEach(line -> msgArea.appendText(line + System.lineSeparator()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
