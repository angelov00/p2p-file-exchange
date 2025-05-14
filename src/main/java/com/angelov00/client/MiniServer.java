package com.angelov00.client;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MiniServer {
    private ServerSocket serverSocket;
    private volatile boolean running;
    private final ExecutorService threadPool;

    private static final int MAX_CLIENTS = 5;

    public MiniServer() {
        this.running = true;
        this.threadPool = Executors.newFixedThreadPool(MAX_CLIENTS);
    }

    public int startAndGetPort() throws IOException {
        serverSocket = new ServerSocket(0); // Автоматичен порт
        return serverSocket.getLocalPort();
    }

    public void start() {
        try {
            while (running) {
                try (Socket client = serverSocket.accept()) {
                    System.out.println("Accepted client connection from " + client.getRemoteSocketAddress());

                    threadPool.submit(() -> {
                        try {
                            handleClient(client);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });

                } catch (IOException e) {
                    if (running) {
                        System.err.println("Error accepting client: " + e.getMessage());
                    }
                }
            }
        } finally {
            stop();
        }
    }

    private void handleClient(Socket client) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
        OutputStream out = client.getOutputStream();

        String request = reader.readLine();
        if (request == null) {
            sendResponse(out, "Error: Empty request");
            return;
        }

        if (!request.startsWith("download ")) {
            sendResponse(out, "Error: Invalid request");
            return;
        }

        String fileName = request.substring("download ".length()).trim();
        File file = new File(fileName);
        if (!file.exists()) {
            sendResponse(out, "Error: File not found: " + fileName);
            return;
        }

        try (FileInputStream fileIn = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fileIn.read(buffer)) > 0) {
                out.write(buffer, 0, bytesRead);
            }
            out.flush();
            System.out.println("Sent file: " + fileName);
        } catch (IOException e) {
            sendResponse(out, "Error: Failed to send file: " + e.getMessage());
        }
    }

    private void sendResponse(OutputStream out, String response) throws IOException {
        out.write((response + "\n").getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    public void stop() {
        running = false;
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
                System.out.println("MiniServer stopped");
            } catch (IOException e) {
                System.err.println("Error closing MiniServer: " + e.getMessage());
            }
        }
    }
}