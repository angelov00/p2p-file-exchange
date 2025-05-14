package com.angelov00.server.command;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.angelov00.server.User;

public class CommandInvoker {
    private final Map<String, Command> commands;
    private final ConcurrentHashMap<String, User> users;

    public CommandInvoker(ConcurrentHashMap<String, User> users) {
        this.users = users;
        this.commands = new HashMap<>();
        commands.put("register", new RegisterCommand(users));
        commands.put("unregister", new UnregisterCommand(users));
        commands.put("list-files", new ListFilesCommand(users));
        commands.put("download", new DownloadCommand());
    }

    public String handleCommand(String commandStr, InetSocketAddress clientAddress) {
        String[] parts = commandStr.trim().split("\\s+", 3);
        if (parts.length == 0) {
            return "Error: Empty command";
        }

        String cmd = parts[0].toLowerCase();
        Command command = commands.get(cmd);
        if (command == null) {
            return "Error: Unknown command";
        }

        Map<String, Object> params = new HashMap<>();
        if (clientAddress != null) {
            params.put("ip", clientAddress.getHostString());
            params.put("port", clientAddress.getPort());
        }

        switch (cmd) {
            case "register":
            case "unregister":
                if (parts.length < 3) {
                    return "Error: Usage: " + cmd + " <user> <file1,file2,...>";
                }
                params.put("username", parts[1]);
                params.put("fileList", parts[2]);
                break;
            case "list-files":
            case "list-users":
                break;
            case "download":
                return command.execute(params);
            default:
                return "Error: Unknown command";
        }

        try {
            return command.execute(params);
        } catch (Exception e) {
            return "Error: Server error. Check logs at logs.txt";
        }
    }
}