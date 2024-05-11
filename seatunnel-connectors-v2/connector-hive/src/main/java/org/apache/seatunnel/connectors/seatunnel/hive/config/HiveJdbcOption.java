package org.apache.seatunnel.connectors.seatunnel.hive.config;

import org.apache.seatunnel.api.configuration.Option;
import org.apache.seatunnel.api.configuration.Options;

/**
 * @author wangls
 * @since 2024/5/13 16:54
 */
public interface HiveJdbcOption {

    Option<String> URL = Options.key("url").stringType().noDefaultValue().withDescription("url");

    Option<String> DRIVER =
            Options.key("driver")
                    .stringType()
                    .defaultValue("org.apache.hive.jdbc.HiveDriver")
                    .withDescription("driver");

    Option<String> USER = Options.key("user").stringType().noDefaultValue().withDescription("user");

    Option<String> PASSWORD =
            Options.key("password").stringType().noDefaultValue().withDescription("password");
}
