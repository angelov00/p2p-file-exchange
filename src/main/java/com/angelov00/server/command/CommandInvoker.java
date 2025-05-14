package com.angelov00.server.command;

import com.angelov00.server.User;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CommandInvoker {

    private final Map<String, Command> commands;
    private final ConcurrentHashMap<String, User> users;

    public CommandInvoker(ConcurrentHashMap<String, User> users) {
        this.users = users;
        this.commands = new HashMap<>();
        commands.put("register", new RegisterCommand());
        commands.put("unregister", new UnregisterCommand());
        commands.put("list-files", new ListFilesCommand());
        commands.put("download", new DownloadCommand());
    }

    public String handleCommand(String command) {
        Command commandObj = commands.get(command);
        return commandObj.execute(command);
    }
}
