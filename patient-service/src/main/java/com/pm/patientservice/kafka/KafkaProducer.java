package com.pm.patientservice.kafka;
import com.pm.patientservice.model.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import patient.events.PatientEvent;

/**
 * Service responsible for producing Kafka events related to patient operations.
 * <p>
 * This class handles the serialization and publishing of patient-related events
 * to Kafka topics. It converts domain objects into Protocol Buffer-based event messages
 * and publishes them to the appropriate Kafka topics for asynchronous processing
 * by other microservices in the system.
 * </p>
 * <p>
 * The producer uses Protocol Buffers (protobuf) for efficient, language-neutral
 * serialization of event data. This ensures compatibility with various consumers
 * and provides a structured contract for the event payload.
 * </p>
 */
@Service
public class KafkaProducer {

    private static final Logger log = LoggerFactory.getLogger(KafkaProducer.class);
    private final KafkaTemplate<String, byte[]> kafkaTemplate;

    /**
     * Constructs a new KafkaProducer with the required dependencies.
     *
     * @param kafkaTemplate The Spring Kafka template used for sending serialized protobuf messages
     */
    public KafkaProducer(KafkaTemplate<String, byte[]> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Sends a patient creation event to the Kafka topic.
     * <p>
     * This method converts a Patient domain object into a PatientEvent protobuf message,
     * serializes it to bytes, and publishes it to the "patient" topic.
     * The event type is set to "PATIENT_CREATED" to indicate the nature of the event.
     * </p>
     * <p>
     * This event can be consumed by other services that need to react to patient creation,
     * such as notification services, analytics services, or audit logging systems.
     * </p>
     *
     * @param patient The patient object containing the data to be published
     */
    public void sendEvent(Patient patient) {
        // Build the Protocol Buffer PatientEvent from the domain object
        PatientEvent event = PatientEvent.newBuilder()
                .setPatientId(patient.getId().toString())
                .setName(patient.getName())
                .setEmail(patient.getEmail())
                .setEventType("PATIENT_CREATED")
                .build();

        try {
            // Send the serialized event to the "patient" topic
            // The event is converted to a byte array for transmission
            kafkaTemplate.send("patient", event.toByteArray());
            log.info("Patient created event sent successfully for patient ID: {}", patient.getId());
        } catch (Exception e) {
            // Log any errors that occur during event publishing
            log.error("Error sending PatientCreated event: {}", event, e);
        }
    }
}