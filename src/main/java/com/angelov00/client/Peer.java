package com.angelov00.client;

import com.angelov00.server.User;
import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.util.*;
import java.util.concurrent.*;

public class Peer {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 7777;
    private static final int BUFFER_SIZE = 1024;
    private final String username;
    private final int miniServerPort;
    private final ConcurrentHashMap<String, User> users;
    private volatile boolean running;
    private final Set<String> registeredFiles;

    public Peer(String username) throws IOException {
        this.username = username;
        this.users = new ConcurrentHashMap<>();
        this.running = true;
        this.registeredFiles = ConcurrentHashMap.newKeySet();
        MiniServer miniServer = new MiniServer();
        this.miniServerPort = miniServer.startAndGetPort();
        new Thread(miniServer::start).start();
    }

    public void start() {
        startMappingUpdater();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            while (running) {
                String input = reader.readLine();
                if (input == null || input.equalsIgnoreCase("exit")) {
                    shutdown();
                    break;
                }
                String response = handleCommand(input.trim());
                System.out.println(response);
            }
        } catch (IOException e) {
            System.err.println("Error reading input: " + e.getMessage());
            shutdown();
        }
    }

    private void startMappingUpdater() {
        new Thread(() -> {
            while (running) {
                try {
                    String response = sendServerCommand("list-users");
                    updateUsersTxt(response);
                    Thread.sleep(30_000);
                } catch (InterruptedException e) {
                    running = false;
                    System.err.println("Mapping updater interrupted");
                } catch (Exception e) {
                    System.err.println("Error updating users.txt: " + e.getMessage());
                }
            }
        }).start();
    }

    private void updateUsersTxt(String response) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("users.txt"))) {
            users.clear();
            if (response.equals("No users available")) {
                writer.write("No users available\n");
                return;
            }
            String[] lines = response.split("\n");
            for (String line : lines) {
                if (line.equals("END") || line.isEmpty()) {
                    continue;
                }
                String[] parts = line.split(" – ");
                if (parts.length == 2) {
                    String username = parts[0].trim();
                    String[] address = parts[1].split(":");
                    if (address.length == 2) {
                        try {
                            String ip = address[0].trim();
                            int port = Integer.parseInt(address[1].trim());
                            users.put(username, new User(username, ip, port));
                            writer.write(line + "\n");
                        } catch (NumberFormatException e) {
                            System.err.println("Invalid port in users list: " + line);
                        }
                    }
                }
            }
            if (users.isEmpty()) {
                writer.write("No users available\n");
            }
        } catch (IOException e) {
            System.err.println("Error writing to users.txt: " + e.getMessage());
        }
    }

    private String handleCommand(String commandStr) {
        String[] parts = commandStr.trim().split("\\s+", 4);
        if (parts.length == 0) {
            return "Error: Empty command";
        }

        String cmd = parts[0].toLowerCase();
        switch (cmd) {
            case "register":
                if (parts.length < 3) {
                    return "Error: Usage: " + cmd + " <username> <file1,file2,...>";
                }
                if (!parts[1].equals(username)) {
                    return "Error: Can only " + cmd + " with own username";
                }
                String[] files = parts[2].split(",");
                for (String file : files) {
                    String filePath = file.trim();
                    if (!new File(filePath).exists()) {
                        return "Error: File does not exist: " + filePath;
                    }
                }
                String registerCommand = commandStr + " " + miniServerPort;
                String response = sendServerCommand(registerCommand);
                if (response.equals("Registration successful")) {
                    Collections.addAll(registeredFiles, files);
                }
                return response;
            case "unregister":
                if (parts.length < 3) {
                    return "Error: Usage: " + cmd + " <username> <file1,file2,...>";
                }
                if (!parts[1].equals(username)) {
                    return "Error: Can only " + cmd + " with own username";
                }
                response = sendServerCommand(commandStr);
                if (response.equals("Unregistration successful")) {
                    Arrays.stream(parts[2].split(",")).forEach(file -> registeredFiles.remove(file.trim()));
                }
                return response;
            case "list-users":
            case "list-files":
                if (parts.length != 1) {
                    return "Error: Usage: " + cmd;
                }
                response = sendServerCommand(commandStr);
                if (cmd.equals("list-users") && !response.equals("No users available")) {
                    updateUsersTxt(response);
                }
                return response;
            case "download":
                if (parts.length != 4) {
                    return "Error: Usage: download <username> <remotePath> <localPath>";
                }
                System.out.println("Current users: " + users.keySet());
                User targetUser = users.get(parts[1]);
                if (targetUser == null) {
                    return "Error: User " + parts[1] + " not found. Try list-users first.";
                }
                return downloadFile(parts[1], parts[2], parts[3]);
            default:
                return "Error: Unknown command";
        }
    }

    private String sendServerCommand(String command) {
        try (SocketChannel serverChannel = SocketChannel.open(new InetSocketAddress(SERVER_HOST, SERVER_PORT))) {
            serverChannel.configureBlocking(true);
            ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
            buffer.put((command + "\n").getBytes(StandardCharsets.UTF_8));
            buffer.flip();
            serverChannel.write(buffer);

            buffer.clear();
            serverChannel.read(buffer);
            buffer.flip();
            return StandardCharsets.UTF_8.decode(buffer).toString().trim();
        } catch (IOException e) {
            return "Error: Failed to communicate with server: " + e.getMessage();
        }
    }

    private String downloadFile(String targetUsername, String remotePath, String localPath) {
        User targetUser = users.get(targetUsername);
        if (targetUser == null) {
            return "Error: User " + targetUsername + " not found. Try list-users first.";
        }

        int port = targetUser.getPort();

        System.out.println("Attempting to connect to " + targetUser.getIp() + ":" + port + " for downloading " + remotePath);

        try (SocketChannel peerChannel = SocketChannel.open(new InetSocketAddress(targetUser.getIp(), port));
             FileOutputStream fos = new FileOutputStream(localPath)) {
            peerChannel.configureBlocking(true);
            ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
            buffer.put(("download " + remotePath + "\n").getBytes(StandardCharsets.UTF_8));
            buffer.flip();
            peerChannel.write(buffer);

            buffer.clear();
            int bytesRead;
            while ((bytesRead = peerChannel.read(buffer)) > 0) {
                buffer.flip();
                byte[] data = new byte[bytesRead];
                buffer.get(data);
                if (new String(data).startsWith("Error")) {
                    return new String(data).trim();
                }
                fos.write(data);
                buffer.clear();
            }
            String registerCommand = "register " + username + " " + localPath + " " + miniServerPort;
            String registerResponse = sendServerCommand(registerCommand);
            if (registerResponse.equals("Registration successful")) {
                registeredFiles.add(localPath);
            } else {
                System.err.println("Warning: Failed to register downloaded file: " + registerResponse);
            }
            return "Download successful: " + localPath;
        } catch (IOException e) {
            return "Error: Failed to download file: " + e.getMessage() + " (Target: " + targetUser.getIp() + ":" + port + ")";
        }
    }

    private void shutdown() {
        running = false;
        if (!registeredFiles.isEmpty()) {
            String filesList = String.join(",", registeredFiles);
            String unregisterCommand = "unregister " + username + " " + filesList;
            String response = sendServerCommand(unregisterCommand);
            if (!response.equals("Unregistration successful")) {
                System.err.println("Warning: Failed to unregister files on shutdown: " + response);
            }
        }
    }

    public static void main(String[] args) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            System.out.print("Enter username: ");
            String username = reader.readLine().trim();
            if (username.isEmpty()) {
                System.out.println("Username cannot be empty");
                return;
            }
            Peer client = new Peer(username);
            client.start();
        } catch (IOException e) {
            System.out.println("Error starting client: " + e.getMessage());
        }
    }
}