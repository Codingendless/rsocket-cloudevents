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

package com.fortycoderplus.rsocket.cloudevents.server;

import static io.rsocket.metadata.WellKnownMimeType.APPLICATION_CLOUDEVENTS_JSON;
import static org.springframework.util.MimeType.valueOf;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import java.net.URI;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@AllArgsConstructor
@Profile("server")
@Controller
public class CloudEventsEndpoint {

    @MessageMapping("cloudevents")
    public Mono<CloudEvent> cloudevents(
            RSocketRequester rsocketRequester, @Header CloudEvent metaCloudEvent, @Payload CloudEvent cloudEvent) {
        logger.info("Received event:{} with metadata:{}", cloudEvent, metaCloudEvent);

        Flux.just(cloudEvent).delayElements(Duration.ofMillis(100)).map(event -> rsocketRequester
                .route("cloudevents")
                .metadata(metadataSpec -> metadataSpec.metadata(
                        CloudEventBuilder.from(metaCloudEvent)
                                .withId(UUID.randomUUID().toString())
                                .withTime(OffsetDateTime.now())
                                .withType("com.fortycoderplus.rsocket.cloudevents.reply")
                                .withSource(URI.create("com.fortycoderplus.rsocket.cloudevents.server"))
                                .build(),
                        valueOf(APPLICATION_CLOUDEVENTS_JSON.getString())))
                .data(event)
                .retrieveFlux(CloudEvent.class)
                .doOnNext(ack -> logger.info("Received replay event:{}", ack)));

        return Mono.just(CloudEventBuilder.from(cloudEvent)
                .withId(UUID.randomUUID().toString())
                .withTime(OffsetDateTime.now())
                .withType("com.fortycoderplus.rsocket.cloudevents.reply")
                .withSource(URI.create("com.fortycoderplus.rsocket.cloudevents.server"))
                .build());
    }
}
