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

package com.fortycoderplus.rsocket.cloudevents.client;

import static io.rsocket.metadata.WellKnownMimeType.APPLICATION_CLOUDEVENTS_JSON;
import static org.springframework.util.MimeType.valueOf;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.stereotype.Component;

@Slf4j
@AllArgsConstructor
@Profile("client")
@Component
public class ClientRunner implements CommandLineRunner {

    private RSocketRequester rsocketRequester;

    @Override
    public void run(String... args) {
        logger.info("Start rsocket client runner");
        rsocketRequester
                .route("cloudevents")
                .metadata(metadataSpec -> metadataSpec.metadata(
                        cloudEvent("com.fortycoderplus.rsocket.cloudevents.metadata"),
                        valueOf(APPLICATION_CLOUDEVENTS_JSON.getString())))
                .data(cloudEvent("com.fortycoderplus.rsocket.cloudevents.push"))
                .retrieveMono(CloudEvent.class)
                .subscribe(cloudEvent -> logger.info("Received reply cloud event:{}", cloudEvent));
    }

    private CloudEvent cloudEvent(String type) {
        return CloudEventBuilder.v1()
                .withId(UUID.randomUUID().toString())
                .withTime(OffsetDateTime.now())
                .withType(type)
                .withSource(URI.create("com.fortycoderplus.rsocket.cloudevents.client"))
                .build();
    }
}
