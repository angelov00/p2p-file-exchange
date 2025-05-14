package com.angelov00.server;

import com.angelov00.server.command.CommandInvoker;
import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;

public class FileServer {
    public static final int SERVER_PORT = 7777;
    private static final String SERVER_HOST = "localhost";
    private static final int BUFFER_SIZE = 1024;
    private static final Logger LOGGER = Logger.getLogger(FileServer.class.getName());
    private final ConcurrentHashMap<String, User> users = new ConcurrentHashMap<>();
    private final CommandInvoker commandInvoker = new CommandInvoker(users);

    public static void main(String[] args) {
        new FileServer().start();
    }

    private void start() {
        setupLogger();
        try (Selector selector = Selector.open();
             ServerSocketChannel serverSocketChannel = ServerSocketChannel.open()) {
            serverSocketChannel.bind(new InetSocketAddress(SERVER_HOST, SERVER_PORT));
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

            System.out.println("Server started on port " + SERVER_PORT);

            while (true) {
                int readyChannels = selector.select();
                if (readyChannels == 0) {
                    continue;
                }

                Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();
                while (keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();
                    try {
                        if (key.isAcceptable()) {
                            acceptClient(selector, serverSocketChannel);
                        } else if (key.isReadable()) {
                            readClient(key);
                        }
                    } catch (IOException e) {
                        logError("Error processing key: " + key, e);
                        closeKey(key);
                    }
                    keyIterator.remove();
                }
            }
        } catch (IOException e) {
            logError("Server startup failed", e);
            System.out.println("Unable to start server. Check logs at logs.txt");
        }
    }

    private void acceptClient(Selector selector, ServerSocketChannel serverChannel) throws IOException {
        SocketChannel client = serverChannel.accept();
        client.configureBlocking(false);
        client.register(selector, SelectionKey.OP_READ);
        LOGGER.info("Accepted client: " + client.getRemoteAddress());
    }

    private void readClient(SelectionKey key) throws IOException {
        SocketChannel client = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);

        int bytesRead = client.read(buffer);
        if (bytesRead < 0) {
            LOGGER.info("Client closed connection: " + client.getRemoteAddress());
            closeKey(key);
            return;
        }

        buffer.flip();
        String receivedData = StandardCharsets.UTF_8.decode(buffer).toString().trim();
        LOGGER.info("Received: " + receivedData + " from " + client.getRemoteAddress());

        String response = commandInvoker.handleCommand(receivedData, (InetSocketAddress) client.getRemoteAddress());
        buffer.clear();
        buffer.put((response).getBytes(StandardCharsets.UTF_8));
        buffer.flip();
        client.write(buffer);
    }

    private void closeKey(SelectionKey key) {
        try {
            SocketChannel client = (SocketChannel) key.channel();
            client.close();
            key.cancel();
            users.entrySet().removeIf(entry -> entry.getValue().getFiles().isEmpty());
            LOGGER.info("Closed connection: " + client.getRemoteAddress());
        } catch (IOException e) {
            logError("Error closing connection", e);
        }
    }

    private void setupLogger() {
        try {
            FileHandler fileHandler = new FileHandler("logs.txt", true);
            fileHandler.setFormatter(new SimpleFormatter());
            LOGGER.addHandler(fileHandler);
            LOGGER.setLevel(Level.ALL);
            LOGGER.setUseParentHandlers(false);
        } catch (IOException e) {
            System.err.println("Failed to setup logger: " + e.getMessage());
        }
    }

    private void logError(String message, Exception e) {
        LOGGER.severe(message + ": " + e.getMessage());
        for (StackTraceElement ste : e.getStackTrace()) {
            LOGGER.severe(ste.toString());
        }
    }
}