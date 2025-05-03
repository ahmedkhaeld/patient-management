package com.pm.analyticsservice.kafka;

import com.google.protobuf.InvalidProtocolBufferException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import patient.events.PatientEvent;

/**
 * Service responsible for consuming and processing patient-related Kafka events.
 * <p>
 * This class listens to the "patient" Kafka topic and processes incoming patient events
 * for analytics purposes. It deserializes Protocol Buffer-based event messages
 * and performs necessary analytics operations based on the event data.
 * </p>
 * <p>
 * The consumer is configured with the group ID "analytics-service" to ensure proper
 * load balancing and fault tolerance when multiple instances of the service are running.
 * </p>
 */
@Service
public class KafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(
            KafkaConsumer.class);

    /**
     * Consumes and processes patient events from the Kafka topic.
     * <p>
     * This method is automatically invoked when a new message arrives on the "patient" topic.
     * It deserializes the Protocol Buffer message into a PatientEvent object and processes
     * it for analytics purposes. The method logs information about the received event
     * and can be extended to perform additional analytics operations.
     * </p>
     *
     * @param event The serialized Protocol Buffer message as a byte array
     */
    @KafkaListener(topics="patient", groupId = "analytics-service")
    public void consumeEvent(byte[] event) {
        try {
            PatientEvent patientEvent = PatientEvent.parseFrom(event);
            // ... perform any business related to analytics here

            log.info("Received Patient Event: [PatientId={},PatientName={},PatientEmail={}]",
                    patientEvent.getPatientId(),
                    patientEvent.getName(),
                    patientEvent.getEmail());
        } catch (InvalidProtocolBufferException e) {
            log.error("Error deserializing event {}", e.getMessage());
        }
    }
}