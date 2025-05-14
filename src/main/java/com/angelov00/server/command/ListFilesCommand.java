package com.angelov00.server.command;

import com.angelov00.server.User;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ListFilesCommand implements Command {

    private final ConcurrentHashMap<String, User> users;

    public ListFilesCommand(ConcurrentHashMap<String, User> users) {
        this.users = users;
    }

    @Override
    public String execute(Map<String, Object> params) {
        StringBuilder sb = new StringBuilder();
        boolean hasFiles = false;

        for(User user : users.values()) {
            for (String file : user.getFiles()) {
                if(!file.isEmpty()) {
                    hasFiles = true;
                    sb.append(user.getUsername()).append(" : ").append(file).append("\n");
                }
            }
        }

        if(!hasFiles) {
            sb.append("No files available!\n");
        }

        return sb.toString();
    }
}
