package com.angelov00.server.command;

import com.angelov00.server.User;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ListUsersCommand implements Command {
    private final ConcurrentHashMap<String, User> users;

    public ListUsersCommand(ConcurrentHashMap<String, User> users) {
        this.users = users;
    }

    @Override
    public String execute(Map<String, Object> params) {
        if (users.isEmpty()) {
            return "No users available";
        }

        StringBuilder response = new StringBuilder();
        for (Map.Entry<String, User> entry : users.entrySet()) {
            User user = entry.getValue();
            response.append(user.getUsername())
                    .append(" – ")
                    .append(user.getIp())
                    .append(":")
                    .append(user.getPort())
                    .append("\n");
        }
        response.append("END");
        return response.toString();
    }
}