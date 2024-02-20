package com.yid.agv.backend.elevator;

import com.yid.agv.backend.ProcessAGVTask;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.Objects;


@Component
public class ElevatorSocketBox {
    private static final Logger log = LoggerFactory.getLogger(ProcessAGVTask.class);
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
    @Value("${elevator.fail_socket}")
    private int ELEVATOR_FAIL_SOCKET;

    private boolean ElevatorBoxConnected;
    private boolean ElevatorBoxManual;
    private boolean ElevatorBoxScan;
    private boolean ElevatorBoxError;
    private boolean ElevatorBoxBuzzer;
    private Thread receiveThread;
    private Thread sendThread;
    private static volatile boolean running = true;
    private Socket socket;
    private BufferedInputStream reader;
    private PrintWriter writer;
    private final ElevatorBoxCommand defaultCommand = ElevatorBoxCommand.ASK_STATUS;
    private int failSocketCount = 0;

//    @PostConstruct
    public void __init() {
        new Thread(this::init).start();
    }

    public void init(){
        connectToServer();
        // 啟動接收訊息的執行緒
        receiveThread = new Thread(() -> {
            try {
                while (running) {
                    // 從伺服器接收訊息
                    String serverMessage = readInputStream(reader);
                    if (serverMessage == null) {
                        // Server has closed the connection
                        log.warn("Server has closed the connection, trying to reconnect...");
                        reStartThread();
                    } else {
//                        log.info("ServerMessage: " + serverMessage);
                        failSocketCount--;
                        if(serverMessage.matches("^QQQ([A-Z]\\d{4})XXX$")){
                            // TODO: update status
                            String resultMode = serverMessage.substring(3, 4);
                            int resultValue = Integer.parseInt(serverMessage.substring(4, 8));
                            boolean[] parseResultValue = parseCommand(resultValue);
                            if ("R".equals(resultMode)) {
//                                ElevatorBoxI??? = Objects.requireNonNull(parseResultValue)[0];
                                ElevatorBoxManual = Objects.requireNonNull(parseResultValue)[1];
                                ElevatorBoxScan = Objects.requireNonNull(parseResultValue)[2];
                                ElevatorBoxError = Objects.requireNonNull(parseResultValue)[3];
                                ElevatorBoxBuzzer = Objects.requireNonNull(parseResultValue)[4];
                            }

                        }
                    }
                }
                log.info("ReceiveThread stop.");
            } catch (SocketException s) {
                reStartThread();
            } catch (IOException e) {
                e.printStackTrace();
                reStartThread();
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
            } catch (InterruptedException ignore) {
//                i.printStackTrace();
            }
        });
        sendThread.start();

        // 設定 ShutdownHook
//        Runtime.getRuntime().addShutdownHook(new Thread(this::cleanup));

    }

    @PreDestroy
    public void cleanup() {
        // 在應用程式終止時執行清理工作
        log.info("Closing Socket...");
        running = false;  // 停止所有執行緒
        try {
            log.info("Closing receiveThread...");
            receiveThread.interrupt();
            log.info("Closing sendThread...");
            sendThread.interrupt();
            sendThread.join();
            if (socket != null && !socket.isClosed()) {
                socket.close();  // 關閉 Socket
            }
            if (reader != null) {
                reader.close();
            }
            if (writer != null) {
                writer.close();  // 關閉 PrintWriter
            }
        } catch (InterruptedException ignore) {
        } catch (IOException e) {
            e.printStackTrace();
        }
        log.info("All SocketThread has been terminated!");
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

            failSocketCount = 0;
            ElevatorBoxConnected = true;
            log.info("Connected to ElevatorBox!");
        } catch (IOException e) {
            log.warn("Failed to connect to server, try again later...");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
            connectToServer();
        }
    }

    private void reStartThread() {
        if(running){
            log.info("Do restart thread...");
            cleanup();
            running = true;
            if(!socket.isClosed()){
                init();
            }
        }
    }

    private String readInputStream(BufferedInputStream _in) throws IOException {
        StringBuilder data = new StringBuilder();
        int s = _in.read();
        if(s==-1) return null;
        data.append((char)s);
        int len = _in.available();
        if(len > 0) {
            byte[] byteData = new byte[len];
            _in.read(byteData);
            data.append(new String(byteData));
        }
        return data.toString();
    }

    private boolean[] parseCommand(int statusValue) {
        if(statusValue < 0) return null;
        boolean[] statusArray = new boolean[8];
        // 從右到左解析各個位元狀態
        for (int i = 0; i < statusArray.length ; i++) {
            // 檢查第i位是否為1，若是則代表狀態為真
            statusArray[i] = (statusValue & 1) == 1;
            // 右移一位，繼續解析下一位元
            statusValue >>= 1;
        }
        return statusArray;
    }

    public synchronized void sendCommandToElevatorBox(ElevatorBoxCommand elevatorBoxCommand){
        if (socket != null && !socket.isClosed()) {
            if(elevatorBoxCommand != ElevatorBoxCommand.ASK_STATUS){
                log.info("ElevatorBox Command: " + elevatorBoxCommand.getCommand());
            }
            writer.println(elevatorBoxCommand.getCommand());
            failSocketCount++;
            if(failSocketCount >= ELEVATOR_FAIL_SOCKET){
                ElevatorBoxConnected = false;
                reStartThread();
            }
        }
    }

    public boolean isElevatorBoxConnected() {
        return ElevatorBoxConnected;
    }

    public boolean isElevatorBoxManual() {
        return ElevatorBoxManual;
    }

    public boolean isElevatorBoxScan() {
        return ElevatorBoxScan;
    }

    public boolean isElevatorBoxError() {
        return ElevatorBoxError;
    }

    public boolean isElevatorBoxBuzzer() {
        return ElevatorBoxBuzzer;
    }
}
