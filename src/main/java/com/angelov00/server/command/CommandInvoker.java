package com.angelov00.server.command;

import java.util.HashMap;
import java.util.Map;

public class CommandInvoker {

    private final Map<String, Command> commands;

    public CommandInvoker() {
        this.commands = new HashMap<>();
        commands.put("register", new RegisterCommand());
        commands.put("unregister", new UnregisterCommand());
        commands.put("list-files", new ListFilesCommand());
        commands.put("download", new DownloadCommand());
    }

    public String handleCommand(String command) {
        return "";
    }
}
