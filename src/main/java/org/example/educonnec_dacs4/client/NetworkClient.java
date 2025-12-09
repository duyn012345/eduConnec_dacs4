package org.example.educonnec_dacs4.client;

import javafx.application.Platform;
import org.example.educonnec_dacs4.model.User;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

public class NetworkClient {

    private static NetworkClient instance;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private Thread listenerThread;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private static final List<BiConsumer<String, String>> messageSubscribers = new ArrayList<>();
   // private BiConsumer<String, String> onMessageReceived;
    private final List<java.util.function.BiConsumer<String, String>> listeners = new ArrayList<>();
    private boolean isListenerRegistered = false;
   // private java.util.function.BiConsumer<String, String> globalListener;
    private User currentUser;
    private java.util.function.BiConsumer<String, String> temporaryControllerListener = null;

    private NetworkClient() {}
    public static NetworkClient getInstance() {
        if (instance == null) {
            instance = new NetworkClient();
        }
        return instance;
    }
    public boolean connect(String ip, int port) {
        try {
            socket = new Socket(ip, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
            startListener();
            System.out.println("Kết nối server thành công: " + ip + ":" + port);
            return true;
        } catch (IOException e) {
            System.err.println("Không thể kết nối server: " + e.getMessage());
            return false;
        }
    }
    private void startListener() {
        running.set(true);
        listenerThread = new Thread(() -> {
            try {
                String line;
                while (running.get() && (line = in.readLine()) != null) {
                    final String message = line.trim();
                    if (message.isEmpty()) continue;

                    final int separatorIndex = message.indexOf('|');
                    final String cmd = separatorIndex == -1 ? message : message.substring(0, separatorIndex);
                    final String payload = separatorIndex == -1 ? "" : message.substring(separatorIndex + 1);
                      Platform.runLater(() -> {
                        // Gọi listener cũ (nếu có)
//                        if (onMessageReceived != null) {
//                            onMessageReceived.accept(cmd, payload);
//                        }
                        // Gọi listener mới (subscribe) – QUAN TRỌNG NHẤT!
//                        if (globalListener != null) {
//                            globalListener.accept(cmd, payload);
//                        }
                        // Gọi tất cả các listener đã subscribe (ChatController, HomeController, v.v.)
                        broadcastMessage(cmd, payload);
                    });
                }
            } catch (IOException e) {
                if (running.get()) {
                    Platform.runLater(() -> System.err.println("Mất kết nối với server: " + e.getMessage()));
                }
            } finally {
                running.set(false);
                Platform.runLater(() -> System.out.println("Listener thread đã dừng."));
            }
        });
        listenerThread.setDaemon(true);
        listenerThread.setName("Network-Listener-Thread");
        listenerThread.start();
    }

    public void send(String message) {
        if (out != null && !socket.isClosed()) {
            out.println(message);
              } else {
            System.err.println("Không thể gửi tin nhắn: Socket đã đóng!");
        }
    }

    public void disconnect() {
        running.set(false);
        try {
            if (out != null) {
                out.println("LOGOUT");
                out.flush();
            }
            Thread.sleep(300); // Đảm bảo server nhận được LOGOUT
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (Exception ignored) {
        } finally {
            currentUser = null;
            isListenerRegistered = false;
            //onMessageReceived = null;
            // FIX LỖI: Dọn dẹp các listeners theo kiến trúc mới
            this.temporaryControllerListener = null; // Clear listener tạm thời
            this.listeners.clear(); // Quan trọng: Xóa tất cả listeners đã đăng ký
            System.out.println("Đã ngắt kết nối và dọn dẹp sạch sẽ.");
        }
    }

     public void subscribe(java.util.function.BiConsumer<String, String> listener) {
        // NẾU ĐÃ CÓ RỒI THÌ KHÔNG THÊM NỮA → TRÁNH LẶP!
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }
    // Trong NetworkClient.java
    public void unsubscribe(java.util.function.BiConsumer<String, String> listener) {
        // Loại bỏ listener khỏi danh sách
        listeners.remove(listener);
    }
    // HÀM GỌI KHI MUỐN CHUYỂN TIẾP TIN NHẮN CHO TẤT CẢ CONTROLLER
    public void broadcastMessage(String cmd, String payload) {
        // GỌI TẤT CẢ CÁC LISTENER ĐÃ ĐĂNG KÝ
        for (var listener : new ArrayList<>(listeners)) { // copy để tránh ConcurrentModification
            try {
                listener.accept(cmd, payload);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    public void setOnMessageReceived(java.util.function.BiConsumer<String, String> listener) {
        //this.globalListener = listener;
        // 1. Loại bỏ listener tạm thời cũ (nếu có) khỏi danh sách broadcast
        if (this.temporaryControllerListener != null) {
            this.listeners.remove(this.temporaryControllerListener);
        }

        // 2. Gán listener mới
        this.temporaryControllerListener = listener;

        // 3. Nếu listener mới không null, thêm nó vào danh sách broadcast
        if (listener != null) {
            subscribe(listener);
        }
    }
    public java.util.function.BiConsumer<String, String> getOnMessageReceived() {
        return this.temporaryControllerListener;
    }
    public boolean isConnected() {
        return socket != null && !socket.isClosed() && socket.isConnected();
    }
    public void setCurrentUser(User user) {
        this.currentUser = user;
    }
    public User getCurrentUser() {
        return currentUser;
    }
    public boolean isLoggedIn() {
        return currentUser != null;
    }
    public String getUsername() {
        return currentUser != null ? currentUser.getUsername() : "Khách";
    }
    public void requestNotifications() {
        send("GET_NOTIFICATIONS");
    }
}