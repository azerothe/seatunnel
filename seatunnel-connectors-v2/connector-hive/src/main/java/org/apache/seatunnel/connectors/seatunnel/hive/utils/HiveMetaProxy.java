package org.apache.seatunnel.connectors.seatunnel.hive.utils;

import org.apache.seatunnel.connectors.seatunnel.hive.meta.HiveTable;

import lombok.NonNull;

import java.util.List;

/**
 * @author wangls
 * @since 2024/5/14 10:22
 */
public interface HiveMetaProxy {
    HiveTable getTable(@NonNull String dbName, @NonNull String tableName);

    void addPartitions(@NonNull String dbName, @NonNull String tableName, List<String> partitions)
            throws Exception;

    void dropPartitions(@NonNull String dbName, @NonNull String tableName, List<String> partitions)
            throws Exception;

    void close();
}
