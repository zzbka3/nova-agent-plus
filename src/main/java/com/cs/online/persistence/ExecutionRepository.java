package com.cs.online.persistence;

import com.cs.online.execution.Execution;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ExecutionRepository {

    private final JdbcTemplate jdbcTemplate;

    public ExecutionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(Execution execution, String resourceId, String contextJson, String errorMessage) {
        jdbcTemplate.update(
                "INSERT INTO execution_record (execution_id, resource_id, status, context_json, error_message) VALUES (?, ?, ?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE status = ?, context_json = ?, error_message = ?",
                execution.executionId(), resourceId, execution.status().name(), contextJson, errorMessage,
                execution.status().name(), contextJson, errorMessage
        );
    }

    public ExecutionRecord findById(String executionId) {
        return jdbcTemplate.query(
                "SELECT execution_id, resource_id, status, context_json, error_message, created_at, updated_at FROM execution_record WHERE execution_id = ?",
                rs -> rs.next()
                        ? new ExecutionRecord(
                                rs.getString("execution_id"),
                                rs.getString("resource_id"),
                                rs.getString("status"),
                                rs.getString("context_json"),
                                rs.getString("error_message"),
                                rs.getTimestamp("created_at").toLocalDateTime(),
                                rs.getTimestamp("updated_at").toLocalDateTime())
                        : null,
                executionId
        );
    }
}
