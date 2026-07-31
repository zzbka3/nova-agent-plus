package com.cs.online.persistence;

import java.time.LocalDateTime;

public record ResourceRecord(String id, String type, String version, String name, String configJson, LocalDateTime createdAt) {
}
