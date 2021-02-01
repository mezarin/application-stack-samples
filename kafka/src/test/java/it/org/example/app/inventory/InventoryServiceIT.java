/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - Initial implementation
 *******************************************************************************/
package it.org.example.app.inventory;

import java.math.BigDecimal;
import java.util.List;
import java.util.Properties;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.example.app.inventory.InventoryResource;
import org.example.app.models.SystemLoad;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.microshed.testing.SharedContainerConfig;
import org.microshed.testing.jaxrs.RESTClient;
import org.microshed.testing.jupiter.MicroShedTest;
import org.microshed.testing.kafka.KafkaProducerClient;

import java.net.InetAddress;
import java.net.UnknownHostException;

@MicroShedTest
@SharedContainerConfig(AppContainerConfig.class)
@TestMethodOrder(OrderAnnotation.class)
public class InventoryServiceIT {

    static {
        System.setProperty("org.microshed.kafka.bootstrap.servers",
                System.getenv("mp.messaging.connector.liberty-kafka.bootstrap.servers"));
    }

    @RESTClient
    public static InventoryResource inventoryResource;

    @KafkaProducerClient(
        valueSerializer = org.example.app.models.SystemLoadSerializer.class, 
        keySerializer = org.apache.kafka.common.serialization.StringSerializer.class)
    public static KafkaProducer<String, SystemLoad> producer;

    @AfterAll
    public static void cleanup() {
        inventoryResource.resetSystems();
    }

    @Test
    public void testCpuUsage() throws InterruptedException, UnknownHostException {
        // Publish a the system load as a base event.
        SystemLoad sl = new SystemLoad(InetAddress.getLocalHost().getHostName(), 1.1);
        producer.send(new ProducerRecord<String, SystemLoad>("systemLoadTopic", sl));
        Thread.sleep(5000);

        // Read the latest published system load and validate it. Iterate for uo to 30 seconds.
        List<Properties> systems= null;
        int status = 0;
        int retryCount = 1;
        while (retryCount <= 6) {
            Response response = inventoryResource.getSystems();
            status = response.getStatus();
            if (status != 200) {
                retryCount++;
                Thread.sleep(5000);
                continue;
            }

            systems = response.readEntity(new GenericType<List<Properties>>() {});

            if (systems.size() == 0) {
                retryCount++;
                Thread.sleep(5000);
                continue;
            } else {
                break;
            }
        }

        Assertions.assertEquals(200, status, "The response should be 200.");
        Assertions.assertEquals(1, systems.size(), "The response should have contained one system' data. It had zero.");

        for (Properties system : systems) {
            Assertions.assertEquals(sl.hostname, system.get("hostname"), "Hostname doesn't match!");
            BigDecimal systemLoad = (BigDecimal) system.get("systemLoad");
            Assertions.assertNotEquals(0.0, systemLoad.doubleValue(), "CPU load should not be 0!");
        }
    }
}
