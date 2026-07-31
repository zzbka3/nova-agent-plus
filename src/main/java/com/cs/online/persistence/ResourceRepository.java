package com.cs.online.persistence;

import com.cs.online.resource.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Resource 只是 Definition，落库时以 id/type/version/name + config_json 快照存储。
 * config_json 由调用方传入序列化后的 JSON 字符串（Resource 本身不关心存储细节）。
 */
@Repository
public class ResourceRepository {

    private final JdbcTemplate jdbcTemplate;

    public ResourceRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(Resource resource, String name, String configJson) {
        jdbcTemplate.update(
                "INSERT INTO resource_definition (id, type, version, name, config_json) VALUES (?, ?, ?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE type = ?, version = ?, name = ?, config_json = ?",
                resource.id(), resource.type().name(), resource.version(), name, configJson,
                resource.type().name(), resource.version(), name, configJson
        );
    }

    public ResourceRecord findById(String id) {
        return jdbcTemplate.query(
                "SELECT id, type, version, name, config_json, created_at FROM resource_definition WHERE id = ?",
                this::mapSingle,
                id
        );
    }

    public List<ResourceRecord> findByType(String type) {
        return jdbcTemplate.query(
                "SELECT id, type, version, name, config_json, created_at FROM resource_definition WHERE type = ? ORDER BY created_at DESC",
                (rs, rowNum) -> new ResourceRecord(
                        rs.getString("id"),
                        rs.getString("type"),
                        rs.getString("version"),
                        rs.getString("name"),
                        rs.getString("config_json"),
                        rs.getTimestamp("created_at").toLocalDateTime()),
                type
        );
    }

    public void delete(String id) {
        jdbcTemplate.update("DELETE FROM resource_definition WHERE id = ?", id);
    }

    private ResourceRecord mapSingle(java.sql.ResultSet rs) throws java.sql.SQLException {
        if (!rs.next()) {
            return null;
        }
        return new ResourceRecord(
                rs.getString("id"),
                rs.getString("type"),
                rs.getString("version"),
                rs.getString("name"),
                rs.getString("config_json"),
                rs.getTimestamp("created_at").toLocalDateTime());
    }
}
