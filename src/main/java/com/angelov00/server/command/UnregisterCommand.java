package com.angelov00.server.command;

import com.angelov00.server.User;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UnregisterCommand implements Command {

    private final ConcurrentHashMap<String, User> users;

    public UnregisterCommand(ConcurrentHashMap<String, User> users) {
        this.users = users;
    }

    @Override
    public String execute(Map<String, Object> params) {
        String username = (String) params.get("username");
        String fileList = (String) params.get("fileList");

        if(username == null || fileList == null) {
            return "Error: Missing parameters for unregister";
        }

        User user = users.get(username);
        if(user == null) {
            return "Error: User not found";
        }

        String[] files = fileList.split(",");
        for (String file : files) {
            String trimmedFile = file.trim();
            if (!trimmedFile.isEmpty()) {
                user.getFiles().remove(trimmedFile);
            }
        }

        return "Unregistration successful";
    }
}
