/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.seatunnel.connectors.seatunnel.kudu;

import org.apache.seatunnel.connectors.seatunnel.kudu.catalog.KuduCatalogFactory;
import org.apache.seatunnel.connectors.seatunnel.kudu.sink.KuduSinkFactory;
import org.apache.seatunnel.connectors.seatunnel.kudu.source.KuduSourceFactory;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class KuduFactoryTest {

    @Test
    void optionRule() {
        Assertions.assertNotNull((new KuduSourceFactory()).optionRule());
        Assertions.assertNotNull((new KuduSinkFactory()).optionRule());
        Assertions.assertNotNull((new KuduCatalogFactory()).optionRule());
    }
}
