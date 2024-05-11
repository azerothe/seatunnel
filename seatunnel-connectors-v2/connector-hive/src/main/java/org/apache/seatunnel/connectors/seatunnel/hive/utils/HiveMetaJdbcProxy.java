package org.apache.seatunnel.connectors.seatunnel.hive.utils;

import org.apache.seatunnel.shade.com.typesafe.config.Config;

import org.apache.seatunnel.common.config.TypesafeConfigUtils;
import org.apache.seatunnel.connectors.seatunnel.file.config.BaseSourceConfigOptions;
import org.apache.seatunnel.connectors.seatunnel.file.hadoop.HadoopLoginFactory;
import org.apache.seatunnel.connectors.seatunnel.hive.config.HadoopAuthConfigOption;
import org.apache.seatunnel.connectors.seatunnel.hive.config.HiveJdbcOption;
import org.apache.seatunnel.connectors.seatunnel.hive.exception.HiveConnectorErrorCode;
import org.apache.seatunnel.connectors.seatunnel.hive.exception.HiveConnectorException;
import org.apache.seatunnel.connectors.seatunnel.hive.meta.HiveTable;

import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.conf.Configuration;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author wangls
 * @since 2024/5/13 16:43
 */
@Slf4j
public class HiveMetaJdbcProxy implements HiveMetaProxy {
    private static volatile HiveMetaJdbcProxy INSTANCE = null;
    private final Connection connection;

    @SneakyThrows
    private HiveMetaJdbcProxy(Config config) {
        String url = config.getString(HiveJdbcOption.URL.key());
        String driver = config.getString(HiveJdbcOption.DRIVER.key());
        String user =
                config.hasPath(HiveJdbcOption.USER.key())
                        ? config.getString(HiveJdbcOption.USER.key())
                        : null;
        String password =
                config.hasPath(HiveJdbcOption.PASSWORD.key())
                        ? config.getString(HiveJdbcOption.PASSWORD.key())
                        : null;
        Class.forName(driver);
        if (HiveMetaStoreProxyUtils.enableKerberos(config)) {
            Configuration kerberosConfiguration = new Configuration();
            kerberosConfiguration.set("hadoop.security.authentication", "Kerberos");
            this.connection =
                    HadoopLoginFactory.loginWithKerberos(
                            kerberosConfiguration,
                            TypesafeConfigUtils.getConfig(
                                    config,
                                    BaseSourceConfigOptions.KRB5_PATH.key(),
                                    BaseSourceConfigOptions.KRB5_PATH.defaultValue()),
                            config.getString(BaseSourceConfigOptions.KERBEROS_PRINCIPAL.key()),
                            config.getString(BaseSourceConfigOptions.KERBEROS_KEYTAB_PATH.key()),
                            config.getString(HadoopAuthConfigOption.LOGIN_CONFIG.key()),
                            config.getString(
                                    HadoopAuthConfigOption.ZOOKEEPER_SERVER_PRINCIPAL.key()),
                            (configuration, userGroupInformation) ->
                                    StringUtils.isAnyBlank(user, password)
                                            ? DriverManager.getConnection(url)
                                            : DriverManager.getConnection(url, user, password));
            return;
        }
        this.connection =
                StringUtils.isAnyBlank(user, password)
                        ? DriverManager.getConnection(url)
                        : DriverManager.getConnection(url, user, password);
    }

    public static HiveMetaJdbcProxy getInstance(Config config) {
        if (INSTANCE == null) {
            synchronized (HiveMetaJdbcProxy.class) {
                if (INSTANCE == null) {
                    INSTANCE = new HiveMetaJdbcProxy(config);
                }
            }
        }
        return INSTANCE;
    }

    public static final String COLUMN_LABEL1 = "col_name";
    public static final String COLUMN_LABEL2 = "data_type";
    public static final String COLUMN_LABEL3 = "comment";

    @Override
    public HiveTable getTable(@NonNull String dbName, @NonNull String tableName) {
        try (PreparedStatement statement =
                        connection.prepareStatement("desc formatted " + dbName + "." + tableName);
                ResultSet resultSet = statement.executeQuery()) {
            ArrayList<HiveTable.HiveColumn> sinkFields = new ArrayList<>();
            ArrayList<HiveTable.HiveColumn> partitionKeys = new ArrayList<>();
            String inputFormat = null;
            String outputFormat = null;
            HashMap<String, String> storageDescParams = new HashMap<>();
            String location = null;
            while (resultSet.next()) {
                String colName = resultSet.getString(COLUMN_LABEL1);
                if ("# col_name".equalsIgnoreCase(colName)) {
                    while (resultSet.next()) {
                        String sinkField = resultSet.getString(COLUMN_LABEL1);
                        if (StringUtils.isNotBlank(sinkField) && !sinkField.startsWith("#")) {
                            sinkFields.add(
                                    new HiveTable.HiveColumn(
                                            sinkField, resultSet.getString(COLUMN_LABEL2)));
                        } else {
                            break;
                        }
                    }
                } else if ("# Partition Information".equalsIgnoreCase(colName)) {
                    resultSet.next();
                    while (resultSet.next()) {
                        String partitionKey = resultSet.getString(COLUMN_LABEL1);
                        if (StringUtils.isNotBlank(partitionKey) && !partitionKey.startsWith("#")) {
                            partitionKeys.add(
                                    new HiveTable.HiveColumn(
                                            partitionKey, resultSet.getString(COLUMN_LABEL2)));
                        } else {
                            break;
                        }
                    }

                } else if ("# Detailed Table Information".equalsIgnoreCase(colName)) {
                    while (resultSet.next()) {
                        String key = resultSet.getString(COLUMN_LABEL1);
                        if (StringUtils.isNotBlank(key) && !key.startsWith("#")) {
                            if (key.startsWith("Location")) {
                                location = resultSet.getString(COLUMN_LABEL2);
                            }
                        } else {
                            break;
                        }
                    }
                } else if ("# Storage Information".equalsIgnoreCase(colName)) {
                    while (resultSet.next()) {
                        String key = resultSet.getString(COLUMN_LABEL1);
                        if (StringUtils.isNotBlank(key) && !key.startsWith("#")) {
                            if (key.startsWith("OutputFormat")) {
                                outputFormat = resultSet.getString(COLUMN_LABEL2);
                            } else if (key.startsWith("InputFormat")) {
                                inputFormat = resultSet.getString(COLUMN_LABEL2);
                            } else if (key.startsWith("Storage Desc Params")) {
                                while (resultSet.next()) {
                                    String prop = resultSet.getString(COLUMN_LABEL2);
                                    String value = resultSet.getString(COLUMN_LABEL3);
                                    if (StringUtils.isNotBlank(prop)) {
                                        storageDescParams.put(prop, value);
                                    } else {
                                        break;
                                    }
                                }
                            }
                        } else {
                            break;
                        }
                    }
                }
            }
            return new HiveTable(
                    sinkFields,
                    partitionKeys,
                    inputFormat,
                    outputFormat,
                    storageDescParams,
                    location);
        } catch (Exception e) {
            String errorMsg =
                    String.format("Get table [%s.%s] information failed", dbName, tableName);
            throw new HiveConnectorException(
                    HiveConnectorErrorCode.GET_HIVE_TABLE_INFORMATION_FAILED, errorMsg, e);
        }
    }

    @Override
    public void addPartitions(
            @NonNull String dbName, @NonNull String tableName, List<String> partitions)
            throws Exception {
        String sql = "alter table " + dbName + "." + tableName + " add partition(%s)";
        try (Statement statement = connection.createStatement()) {
            for (String partition : partitions) {
                String join = StringUtils.join(partition.split(Pattern.quote("/")), ",");
                try {
                    statement.execute(String.format(sql, join));
                } catch (SQLException e) {
                    log.error("Hive表添加分区失败：" + e.getMessage());
                }
            }
        }
    }

    @Override
    public void dropPartitions(
            @NonNull String dbName, @NonNull String tableName, List<String> partitions)
            throws Exception {
        String sql = "alter table " + dbName + "." + tableName + " drop partition(%s)";
        try (Statement statement = connection.createStatement()) {
            for (String partition : partitions) {
                String join = StringUtils.join(partition.split(Pattern.quote("/")), ",");
                try {
                    statement.execute(String.format(sql, join));
                } catch (SQLException e) {
                    log.error("Hive表删除分区失败：" + e.getMessage());
                }
            }
        }
    }

    @SneakyThrows
    @Override
    public synchronized void close() {
        if (connection != null) {
            connection.close();
            INSTANCE = null;
        }
    }
}
