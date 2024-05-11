package org.apache.seatunnel.connectors.seatunnel.hive.meta;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * @author wangls
 * @since 2024/5/14 13:14
 */
@Data
@AllArgsConstructor
public class HiveTable implements Serializable {
    @NonNull private List<HiveColumn> sinkFields;
    @NonNull private List<HiveColumn> partitionKeys;
    @NonNull private String inputFormat;
    @NonNull private String outputFormat;
    @NonNull private Map<String, String> storageDescParams;
    @NonNull private String location;

    @Data
    @AllArgsConstructor
    public static class HiveColumn implements Serializable {
        @NonNull private String name;
        @NonNull private String type;
    }
}
