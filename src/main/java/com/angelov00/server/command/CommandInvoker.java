package com.angelov00.server.command;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.angelov00.server.User;

public class CommandInvoker {
    private final Map<String, Command> commands;

    public CommandInvoker(ConcurrentHashMap<String, User> users) {
        this.commands = new HashMap<>();
        commands.put("register", new RegisterCommand(users));
        commands.put("unregister", new UnregisterCommand(users));
        commands.put("list-files", new ListFilesCommand(users));
        commands.put("list-users", new ListUsersCommand(users));
    }

    public String handleCommand(String commandStr, InetSocketAddress clientAddress) {
        String[] parts = commandStr.trim().split("\\s+", 4);
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
                if (parts.length < 4) {
                    return "Error: Usage: " + cmd + " <user> <file1,file2,...> <port>";
                }
                params.put("username", parts[1]);
                params.put("fileList", parts[2]);
                try {
                    params.put("port", Integer.parseInt(parts[3])); // Използваме коректния порт
                } catch (NumberFormatException e) {
                    return "Error: Invalid port number";
                }
                break;
            case "list-files":
            case "list-users":
                break;
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