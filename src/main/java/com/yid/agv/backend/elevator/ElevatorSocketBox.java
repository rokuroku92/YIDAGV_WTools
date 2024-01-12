package com.yid.agv.backend.elevator;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;


@Component
public class ElevatorSocketBox {
    public enum ElevatorBoxCommand{
        ASK_STATUS("QQQE0010E0000XXX"), OPEN_BUZZER("QQQE0010E0042XXX"), CLOSE_BUZZER("QQQE0010E0002XXX"), CLOSE_BUTTON("QQQE0010E0021XXX");
        private final String command;
        ElevatorBoxCommand(String command) {
            this.command = command;
        }
        public String getCommand(){
            return command;
        }
    }

    @Value("${elevator.ip}")
    private String ELEVATOR_IP;
    @Value("${elevator.port}")
    private int ELEVATOR_PORT;
    @Value("${elevator.timeout}")
    private int ELEVATOR_TIMEOUT;

    private Thread receiveThread;
    private Thread sendThread;
    private static volatile boolean running = true;
    private Socket socket;
    private BufferedInputStream reader;
    private PrintWriter writer;
    private final ElevatorBoxCommand defaultCommand = ElevatorBoxCommand.ASK_STATUS;

    @PostConstruct
    public void init() {
        connectToServer();

        // 啟動接收訊息的執行緒
        receiveThread = new Thread(() -> {
            try {
                while (running) {
                    // 從伺服器接收訊息
                    String serverMessage = readInputStream(reader);
                    if (serverMessage == null) {
                        // Server has closed the connection
                        System.out.println("Server has closed the connection, trying to reconnect...");
                        connectToServer();
                    } else {
                        System.out.println("ServerMessage: " + serverMessage);
                        if(serverMessage.matches("^QQQ([A-Z]\\d{4})XXX$")){
                            // TODO: update status
                        }
                    }
                }
                System.out.println("ReceiveThread stop.");
            } catch (SocketException s) {
                if(running){
                    receiveThread.interrupt();
                    receiveThread.start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        receiveThread.setDaemon(false);
        receiveThread.start();

        // 啟動發送訊息的執行緒
        sendThread = new Thread(() -> {
            try {
                while (running) {
                    sendCommandToElevatorBox(defaultCommand);
                    Thread.sleep(1000);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        sendThread.start();

        // 設定 ShutdownHook
//        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
//            System.out.println("Closing Socket...");
//            running = false;  // 停止所有執行緒
//            try {
//                receiveThread.interrupt();
//                System.out.println("Closing receiveThread...");
//                sendThread.join();
//                System.out.println("Closing sendThread...");
//                if (socket != null && !socket.isClosed()) {
//                    socket.close();  // 關閉 Socket
//                }
//                if (reader != null) {
//                    reader.close();
//                }
//                if (writer != null) {
//                    writer.close();  // 關閉 PrintWriter
//                }
//            } catch (InterruptedException | IOException e) {
//                e.printStackTrace();
//            }
//            System.out.println("All SocketThread has been terminated!");
//        }));
    }

    @PreDestroy
    public void cleanup() {
        // 在應用程式終止時執行清理工作
        System.out.println("Closing Socket...");
        running = false;  // 停止所有執行緒
        try {
            receiveThread.interrupt();
            System.out.println("Closing receiveThread...");
            sendThread.join();
            System.out.println("Closing sendThread...");
            if (socket != null && !socket.isClosed()) {
                socket.close();  // 關閉 Socket
            }
            if (reader != null) {
                reader.close();
            }
            if (writer != null) {
                writer.close();  // 關閉 PrintWriter
            }
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
        System.out.println("All SocketThread has been terminated!");
    }

    private void connectToServer() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }

            socket = new Socket();
            socket.connect(new InetSocketAddress(ELEVATOR_IP, ELEVATOR_PORT), ELEVATOR_TIMEOUT);

            // 建立輸入流，用於從伺服器讀取資料
            InputStream inputStream = socket.getInputStream();
            reader = new BufferedInputStream(inputStream);

            // 建立輸出流，用於向伺服器發送資料
            OutputStream outputStream = socket.getOutputStream();
            writer = new PrintWriter(outputStream, true);

            System.out.println("Connected to ElevatorBox!");
        } catch (IOException e) {
            System.out.println("Failed to connect to server, try again later...");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
            connectToServer();
        }
    }

    private String readInputStream(BufferedInputStream _in) throws IOException {
//        StringBuilder data = new StringBuilder();
//        int s;
//        while ((s = _in.read()) != -1) {
//            data.append((char) s);
//        }
//        return data.length() > 0 ? data.toString() : null;
        String data = "";
        int s = _in.read();
        if(s==-1) return null;
        data += ""+(char)s;
        int len = _in.available();
        if(len > 0) {
            byte[] byteData = new byte[len];
            _in.read(byteData);
            data += new String(byteData);
        }
        return data;
    }

    public synchronized void sendCommandToElevatorBox(ElevatorBoxCommand elevatorBoxCommand){
        if (socket != null && !socket.isClosed()) {
            System.out.println("ElevatorBox Command: " + elevatorBoxCommand.getCommand());
            writer.println(elevatorBoxCommand.getCommand());
        }
    }
}
