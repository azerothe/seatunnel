package org.apache.seatunnel.connectors.seatunnel.hive.commit;

import org.apache.seatunnel.api.configuration.Option;
import org.apache.seatunnel.api.configuration.Options;

/**
 * @author wangls
 * @since 2024/5/8 13:26
 */
public interface HadoopAuthConfigOption {

    Option<String> LOGIN_CONFIG =
            Options.key("login_config")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("login_config file path");

    Option<String> ZOOKEEPER_SERVER_PRINCIPAL =
            Options.key("zookeeper_server_principal")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("zookeeper_server_principal file path");
}
