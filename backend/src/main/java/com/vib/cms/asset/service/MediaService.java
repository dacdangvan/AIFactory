package com.vib.cms.asset.service;

import com.vib.cms.asset.storage.AssetStorageProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

@Service
public class MediaService {
    private static final long MAX_FILE_SIZE = 20L * 1024 * 1024;
    private static final Set<String> ALLOWED_TYPES = Set.of(
        "image/jpeg", "image/png", "image/gif", "image/webp",
        "application/pdf", "text/plain", "text/csv",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.ms-excel",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "application/vnd.ms-powerpoint",
        "application/vnd.openxmlformats-officedocument.presentationml.presentation",\n        "video/mp4", "video/webm", "audio/mpeg", "audio/mp4", "audio/wav", "audio/ogg"
    );
    private final JdbcTemplate jdbc;
    private final AssetStorageProvider storage;

    public MediaService(JdbcTemplate jdbc, AssetStorageProvider storage) {
        this.jdbc = jdbc;
        this.storage = storage;
    }

    @Transactional
    public Map<String,Object> upload(MultipartFile file, UUID productId, String actor) throws IOException {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Please select a file");
        if (file.getSize() > MAX_FILE_SIZE) throw new IllegalArgumentException("Maximum file size is 20 MB");
        String contentType = Optional.ofNullable(file.getContentType()).orElse("application/octet-stream").toLowerCase(Locale.ROOT);
        if (!ALLOWED_TYPES.contains(contentType)) throw new IllegalArgumentException("File type is not allowed: " + contentType);
        if (productId != null && jdbc.queryForObject("SELECT count(*) FROM products WHERE id=?", Long.class, productId) == 0L)
            throw new IllegalArgumentException("Product does not exist");

        String original = Optional.ofNullable(file.getOriginalFilename()).orElse("upload");
        original = original.replace("\\", "/");
        original = original.substring(original.lastIndexOf('/') + 1).replaceAll("[\\r\\n\\\\]", "_");
        String extension = "";
        int dot = original.lastIndexOf('.');
        if (dot >= 0 && dot < original.length() - 1) extension = original.substring(dot).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9.]", "");
        UUID id = UUID.randomUUID();
        String objectKey = LocalDate.now() + "/" + id + extension;
        storage.upload(file.getInputStream(), objectKey, contentType, file.getSize());
        try {
            jdbc.update("""
                INSERT INTO cms_assets(id,product_id,original_filename,object_key,content_type,file_size,created_by)
                VALUES (?,?,?,?,?,?,?)
                """, id, productId, original, objectKey, contentType, file.getSize(), actor);
        } catch (RuntimeException ex) {
            try { storage.delete(objectKey); } catch (IOException ignored) {}
            throw ex;
        }
        return get(id);
    }

    public List<Map<String,Object>> list(UUID productId, String keyword, int limit) {
        if (limit < 1 || limit > 200) throw new IllegalArgumentException("limit must be between 1 and 200");
        StringBuilder sql = new StringBuilder("""
            SELECT id, product_id AS "productId", original_filename AS "name",
                   object_key AS "objectKey", content_type AS "contentType",
                   file_size AS "size", created_by AS "createdBy", created_at AS "createdAt"
            FROM cms_assets WHERE deleted_at IS NULL
            """);
        List<Object> args = new ArrayList<>();
        if (productId != null) { sql.append(" AND (product_id=? OR product_id IS NULL)"); args.add(productId); }
        if (keyword != null && !keyword.isBlank()) { sql.append(" AND lower(original_filename) LIKE ?"); args.add("%"+keyword.toLowerCase(Locale.ROOT)+"%"); }
        sql.append(" ORDER BY created_at DESC LIMIT ?"); args.add(limit);
        List<Map<String,Object>> rows = jdbc.queryForList(sql.toString(), args.toArray());
        rows.forEach(row -> row.put("url", "/api/v1/media/" + row.get("id") + "/file"));
        return rows;
    }

    public Map<String,Object> get(UUID id) {
        Map<String,Object> row = jdbc.queryForMap("""
            SELECT id, product_id AS "productId", original_filename AS "name",
                   object_key AS "objectKey", content_type AS "contentType",
                   file_size AS "size", created_by AS "createdBy", created_at AS "createdAt"
            FROM cms_assets WHERE id=? AND deleted_at IS NULL
            """, id);
        row.put("url", "/api/v1/media/" + id + "/file");
        return row;
    }

    public StoredAsset getForDownload(UUID id) {
        return jdbc.queryForObject("""
            SELECT object_key, original_filename, content_type, file_size
            FROM cms_assets WHERE id=? AND deleted_at IS NULL
            """, (rs,n) -> new StoredAsset(rs.getString(1),rs.getString(2),rs.getString(3),rs.getLong(4)), id);
    }

    @Transactional
    public void delete(UUID id) {
        jdbc.update("UPDATE cms_assets SET deleted_at=now() WHERE id=? AND deleted_at IS NULL", id);
    }

    public record StoredAsset(String objectKey, String filename, String contentType, long size) {}
}
