package com.angelov00.server.command;

import com.angelov00.server.User;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RegisterCommand implements Command {
    private final ConcurrentHashMap<String, User> users;

    public RegisterCommand(ConcurrentHashMap<String, User> users) {
        this.users = users;
    }

    @Override
    public String execute(Map<String, Object> params) {
        String username = (String) params.get("username");
        String fileList = (String) params.get("fileList");
        String ip = (String) params.get("ip");
        Integer port = (Integer) params.get("port");

        if (username == null || fileList == null || ip == null || port == null) {
            return "Error: Missing parameters for register";
        }

        User user = users.computeIfAbsent(username, k -> new User(username, ip, port));
        String[] files = fileList.split(",");
        for (String file : files) {
            user.getFiles().add(file.trim());
        }
        return "Registration successful";
    }
}