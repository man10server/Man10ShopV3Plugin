package com.shojabon.man10socket;

import org.bukkit.plugin.java.JavaPlugin;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class Man10Socket {
    private static final long REPLY_TIMEOUT_MS = 5000L;
    private static final String MESSAGE_DELIMITER = "<E>";

    private static final ConcurrentHashMap<UUID, ClientConnection> clients = new ConcurrentHashMap<>();
    private static final BlockingQueue<JSONObject> sendQueue = new LinkedBlockingQueue<>();
    private static final ConcurrentHashMap<String, CompletableFuture<JSONObject>> replyWaiters = new ConcurrentHashMap<>();

    private static volatile boolean running = false;
    private static volatile ServerSocket welcomeSocket;
    private static volatile JavaPlugin plugin;

    private Man10Socket() {
    }

    public static synchronized void initialize(JavaPlugin javaPlugin, int port) {
        if (running) {
            return;
        }
        plugin = javaPlugin;
        running = true;

        Thread acceptThread = new Thread(() -> acceptLoop(port), "Man10Socket-Accept");
        acceptThread.setDaemon(true);
        acceptThread.start();

        Thread senderThread = new Thread(Man10Socket::senderLoop, "Man10Socket-Sender");
        senderThread.setDaemon(true);
        senderThread.start();
    }

    public static synchronized void shutdown() {
        running = false;

        if (welcomeSocket != null && !welcomeSocket.isClosed()) {
            try {
                welcomeSocket.close();
            } catch (IOException ignored) {
            }
        }

        for (ClientConnection client : clients.values()) {
            client.close();
        }
        clients.clear();

        for (Map.Entry<String, CompletableFuture<JSONObject>> e : replyWaiters.entrySet()) {
            e.getValue().complete(errorResponse("socket_shutdown", "Socket server was stopped."));
        }
        replyWaiters.clear();
        sendQueue.clear();
    }

    public static JSONObject send(JSONObject message, Boolean reply) {
        if (message == null) {
            return errorResponse("invalid_message", "Message is null.");
        }
        if (!running) {
            return errorResponse("socket_not_running", "Socket server is not initialized.");
        }

        JSONObject payload = new JSONObject(message.toString());
        boolean needsReply = Boolean.TRUE.equals(reply);

        if (!needsReply) {
            sendQueue.add(payload);
            return null;
        }

        String replyId = payload.has("replyId") ? payload.getString("replyId") : UUID.randomUUID().toString();
        payload.put("replyId", replyId);

        CompletableFuture<JSONObject> future = new CompletableFuture<>();
        replyWaiters.put(replyId, future);
        sendQueue.add(payload);

        try {
            JSONObject response = future.get(REPLY_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            return response != null ? response : errorResponse("socket_no_reply", "Reply payload was empty.");
        } catch (TimeoutException e) {
            return errorResponse("socket_timeout", "Timed out while waiting for socket reply.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return errorResponse("socket_interrupted", "Socket request wait was interrupted.");
        } catch (Exception e) {
            return errorResponse("socket_error", "Socket request failed: " + e.getMessage());
        } finally {
            replyWaiters.remove(replyId);
        }
    }

    private static void acceptLoop(int port) {
        try {
            welcomeSocket = new ServerSocket(port);
            log("Socket server listening on port " + port);

            while (running) {
                Socket connectionSocket = welcomeSocket.accept();
                UUID id = UUID.randomUUID();
                ClientConnection client = new ClientConnection(id, connectionSocket);
                clients.put(id, client);
                client.start();
            }
        } catch (IOException e) {
            if (running) {
                log("Socket accept loop stopped: " + e.getMessage());
            }
        }
    }

    private static void senderLoop() {
        while (running) {
            try {
                JSONObject message = sendQueue.poll(1000L, TimeUnit.MILLISECONDS);
                if (message == null) {
                    continue;
                }
                sendInternal(message);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                log("Socket sender loop error: " + e.getMessage());
            }
        }
    }

    private static void sendInternal(JSONObject message) {
        if (clients.isEmpty()) {
            return;
        }
        String target = message.has("target") ? message.optString("target", null) : null;
        for (ClientConnection client : clients.values()) {
            if (target != null) {
                String name = client.getName();
                if (name == null || !name.equalsIgnoreCase(target)) {
                    continue;
                }
            }
            client.send(message);
            return;
        }
    }

    private static void onClientMessage(UUID clientId, JSONObject message) {
        String type = message.optString("type", "");
        if ("set_name".equalsIgnoreCase(type)) {
            ClientConnection client = clients.get(clientId);
            if (client != null) {
                client.setName(message.optString("name", null));
            }
            return;
        }

        if ("reply".equalsIgnoreCase(type)) {
            String replyId = message.optString("replyId", null);
            if (replyId == null) {
                return;
            }
            CompletableFuture<JSONObject> future = replyWaiters.remove(replyId);
            if (future != null) {
                future.complete(message);
            }
        }
    }

    private static void onClientClose(UUID clientId) {
        clients.remove(clientId);
    }

    private static JSONObject errorResponse(String status, String message) {
        JSONObject obj = new JSONObject();
        obj.put("status", status);
        obj.put("message", message);
        return obj;
    }

    private static void log(String message) {
        if (plugin != null) {
            plugin.getLogger().info("[Man10Socket] " + message);
        }
    }

    private static final class ClientConnection {
        private final UUID id;
        private final Socket socket;
        private final BlockingQueue<JSONObject> queue = new LinkedBlockingQueue<>();
        private volatile boolean connected = true;
        private volatile String name;

        private ClientConnection(UUID id, Socket socket) {
            this.id = id;
            this.socket = socket;
        }

        private void start() {
            Thread reader = new Thread(this::readerLoop, "Man10Socket-Reader-" + id);
            reader.setDaemon(true);
            reader.start();

            Thread writer = new Thread(this::writerLoop, "Man10Socket-Writer-" + id);
            writer.setDaemon(true);
            writer.start();
        }

        private void readerLoop() {
            try {
                byte[] buffer = new byte[1024];
                StringBuilder messageBuilder = new StringBuilder();
                InputStream inputStream = socket.getInputStream();

                int bytesRead;
                while (connected && (bytesRead = inputStream.read(buffer)) != -1) {
                    String received = new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);
                    messageBuilder.append(received);
                    if (!messageBuilder.toString().contains(MESSAGE_DELIMITER)) {
                        continue;
                    }
                    while (messageBuilder.toString().contains(MESSAGE_DELIMITER)) {
                        String[] messages = messageBuilder.toString().split(MESSAGE_DELIMITER, 2);
                        try {
                            JSONObject message = new JSONObject(messages[0]);
                            onClientMessage(id, message);
                        } catch (Exception ignored) {
                        } finally {
                            if (messages.length == 1) {
                                messageBuilder = new StringBuilder();
                            } else {
                                messageBuilder = new StringBuilder(messages[1]);
                            }
                        }
                    }
                }
            } catch (IOException ignored) {
            } finally {
                close();
            }
        }

        private void writerLoop() {
            while (connected && running) {
                try {
                    JSONObject message = queue.poll(1000L, TimeUnit.MILLISECONDS);
                    if (message == null) {
                        continue;
                    }
                    DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                    byte[] bytes = (message.toString() + MESSAGE_DELIMITER).getBytes(StandardCharsets.UTF_8);
                    out.write(bytes);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    break;
                }
            }
            close();
        }

        private void send(JSONObject message) {
            queue.add(message);
        }

        private String getName() {
            return name;
        }

        private void setName(String name) {
            this.name = name;
        }

        private void close() {
            connected = false;
            try {
                if (!socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException ignored) {
            }
            onClientClose(id);
        }
    }
}
