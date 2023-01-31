/*
 * (c) Copyright 2023 40CoderPlus. All rights reserved.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.fortycoderplus.rsocket.cloudevents.infrastructure;

import static io.rsocket.metadata.WellKnownMimeType.APPLICATION_CLOUDEVENTS_JSON;
import static org.springframework.util.MimeType.valueOf;

import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.RSocketStrategies;
import reactor.util.retry.Retry;

@Profile("client")
@Configuration
@EnableConfigurationProperties(RSocketCloudEventsClientProperties.class)
public class RSocketCloudEventsClientConfigure {

    @Bean
    public RSocketRequester cloudEventRsocketRequester(
            RSocketCloudEventsClientProperties properties, RSocketStrategies rsocketStrategies) {
        RSocketRequester.Builder builder = RSocketRequester.builder();
        return builder.rsocketStrategies(rsocketStrategies)
                .rsocketConnector(connector -> connector.reconnect(Retry.fixedDelay(2, Duration.ofSeconds(2))))
                .dataMimeType(valueOf(APPLICATION_CLOUDEVENTS_JSON.getString()))
                .tcp(properties.server().host(), properties.server().port());
    }
}
