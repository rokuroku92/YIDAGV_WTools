package com.yid.agv.backend.elevator;

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


/**
 * ElevatorSocketBox 類用於與電梯控制盒建立 Socket 連接並通信。
 */
@Component
public class ElevatorSocketBox {
    private static final Logger log = LoggerFactory.getLogger(ElevatorSocketBox.class);

    /**
     * 定義 ElevatorBoxCommand 列舉類，用於表示要向電梯控制盒發送的命令。
     */
    public enum ElevatorBoxCommand {
        ASK_STATUS("QQQE0010E0000XXX"), OPEN_BUZZER("QQQE0010E0042XXX"), CLOSE_BUZZER("QQQE0010E0002XXX"), CLOSE_BUTTON("QQQE0010E0021XXX");
        private final String command;
        ElevatorBoxCommand(String command) {
            this.command = command;
        }
        public String getCommand(){
            return command;
        }
    }

    /**
     * 定義 ElevatorBoxStatus 列舉類，用於表示電梯控制盒的狀態。
     */
    public enum ElevatorBoxStatus {
        TRUE, FALSE, UNKNOWN
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
    private ElevatorBoxStatus ElevatorBoxManual;
    private ElevatorBoxStatus ElevatorBoxScan;
    private ElevatorBoxStatus ElevatorBoxError;
    private ElevatorBoxStatus ElevatorBoxBuzzer;
    private Thread receiveThread;
    private Thread sendThread;
    private static volatile boolean running = true;
    private Socket socket;
    private BufferedInputStream reader;
    private PrintWriter writer;
    private final ElevatorBoxCommand defaultCommand = ElevatorBoxCommand.ASK_STATUS;
    private int failSocketCount = 0;

    /**
     * 在 Spring 容器初始化後執行，用於初始化 ElevatorSocketBox 對象。
     */
    @PostConstruct
    public void initialize() {
        ElevatorBoxConnected = false;
        ElevatorBoxManual = ElevatorBoxStatus.UNKNOWN;
        ElevatorBoxScan = ElevatorBoxStatus.UNKNOWN;
        ElevatorBoxError = ElevatorBoxStatus.UNKNOWN;
        ElevatorBoxBuzzer = ElevatorBoxStatus.UNKNOWN;
        new Thread(this::elevatorSocketBoxMain).start();
    }

    /**
     * elevatorSocketBoxMain 方法用於執行與電梯控制盒的主要通信邏輯。
     */
    public void elevatorSocketBoxMain() {
        try {
            connectToServer();

            // Check and log thread status before starting new threads
            if (receiveThread != null && receiveThread.isAlive()) {
                log.warn("Receive thread is still running. Stopping it before starting a new one.");
                receiveThread.interrupt();
                receiveThread.join();
            }

            if (sendThread != null && sendThread.isAlive()) {
                log.warn("Send thread is still running. Stopping it before starting a new one.");
                sendThread.interrupt();
                sendThread.join();
            }

            // 啟動接收訊息的執行緒
            receiveThread = new Thread(() -> {
                log.info("receiveThread");
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
                                // update status
                                String resultMode = serverMessage.substring(3, 4);
                                int resultValue = Integer.parseInt(serverMessage.substring(4, 8));
                                boolean[] parseResultValue = parseCommand(resultValue);
                                if ("R".equals(resultMode)) {
//                                ElevatorBoxI??? = Objects.requireNonNull(parseResultValue)[0];
                                    ElevatorBoxManual = Objects.requireNonNull(parseResultValue)[1] ? ElevatorBoxStatus.TRUE : ElevatorBoxStatus.FALSE;
                                    ElevatorBoxScan = Objects.requireNonNull(parseResultValue)[2] ? ElevatorBoxStatus.TRUE : ElevatorBoxStatus.FALSE;
                                    ElevatorBoxError = Objects.requireNonNull(parseResultValue)[3] ? ElevatorBoxStatus.TRUE : ElevatorBoxStatus.FALSE;
                                    ElevatorBoxBuzzer = Objects.requireNonNull(parseResultValue)[4] ? ElevatorBoxStatus.TRUE : ElevatorBoxStatus.FALSE;
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
                log.info("sendThread");
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

        } catch (Exception e) {
            log.error("Exception in elevatorSocketBoxMain: ", e);
            reStartThread();
        }

        // 設定 ShutdownHook
//        Runtime.getRuntime().addShutdownHook(new Thread(this::cleanup));

    }

    /**
     * 在 Spring 容器銷毀之前執行，用於清理資源。
     */
    @PreDestroy
    public void cleanup() {
        // 在應用程式終止時執行清理工作
        log.info("Closing Socket...");
        running = false;  // 停止所有執行緒
        try {
            if (receiveThread != null) {
                log.info("Closing receiveThread...");
                receiveThread.interrupt();
                log.info("Closing sendThread...");
                sendThread.interrupt();
                sendThread.join();
            } else {
                log.warn("Unsuccessful connection to ElevatorSocketBox");
            }
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

    private boolean iCon = true;
    private void connectToServer() {
        try {
            if (socket != null && !socket.isClosed()) {
                log.info("Closing existing socket connection...");
                socket.close();
            }

            log.info("Attempting to connect to ElevatorBox at IP: {} Port: {}", ELEVATOR_IP, ELEVATOR_PORT);
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
            iCon=true;
        } catch (IOException e) {
            log.warn("Failed to connect to ElevatorSocketBox: ", e);
            if (iCon) {
                log.warn("Failed to connect to ElevatorSocketBox, try again later...");
                iCon = false;
            }
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
            log.info("Starting elevatorSocketBoxMain after cleanup...");
            elevatorSocketBoxMain();
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

    /**
     * 向電梯控制盒發送命令的方法。
     * @param elevatorBoxCommand 要發送的命令
     */
    public synchronized void sendCommandToElevatorBox(ElevatorBoxCommand elevatorBoxCommand){
        if (socket != null && !socket.isClosed() && writer != null) {
            if(elevatorBoxCommand != ElevatorBoxCommand.ASK_STATUS){
                log.info("ElevatorBox Command: {}", elevatorBoxCommand.getCommand());
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

    public ElevatorBoxStatus isElevatorBoxManual() {
        return ElevatorBoxConnected ? ElevatorBoxManual : ElevatorBoxStatus.UNKNOWN;
    }

    public ElevatorBoxStatus isElevatorBoxScan() {
        return ElevatorBoxConnected ? ElevatorBoxScan : ElevatorBoxStatus.UNKNOWN;
    }

    public ElevatorBoxStatus isElevatorBoxError() {
        return ElevatorBoxConnected ? ElevatorBoxError : ElevatorBoxStatus.UNKNOWN;
    }

    public ElevatorBoxStatus isElevatorBoxBuzzer() {
        return ElevatorBoxConnected ? ElevatorBoxBuzzer : ElevatorBoxStatus.UNKNOWN;
    }
}
