package com.cs.online.context;

import java.util.ArrayList;
import java.util.List;

public class Conversation {

    private final List<Message> messages = new ArrayList<>();

    public void addUserMessage(String content) {
        messages.add(new Message("user", content));
    }

    public void addAssistantMessage(String content) {
        messages.add(new Message("assistant", content));
    }

    public void addSystemMessage(String content) {
        messages.add(new Message("system", content));
    }

    public List<Message> messages() {
        return messages;
    }

    public record Message(String role, String content) {
    }
}
