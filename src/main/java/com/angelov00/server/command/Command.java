package com.angelov00.server.command;

import java.util.Map;

public interface Command {
    String execute(Map<String, Object> params);
}
