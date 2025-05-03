package com.pm.patientservice.grpc;

import billing.BillingRequest;
import billing.BillingResponse;
import billing.BillingServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * gRPC client for communicating with the Billing Service.
 * <p>
 * Handles the creation of managed channels and provides a blocking stub for unary RPC calls.
 * Configured through Spring's value injection for service address and port.
 * </p>
 *
 * @see BillingServiceGrpc.BillingServiceBlockingStub
 * @see ManagedChannel
 */
@Service
public class BillingServiceGrpcClient {

    private static final Logger log = LoggerFactory.getLogger(BillingServiceGrpcClient.class);

    /**
     * gRPC blocking stub for synchronous unary calls to BillingService.
     * Provides thread-safe communication with the billing service's
     * CreateBillingAccount RPC endpoint.
     *
     * @see #createBillingAccount(String, String, String) Main entry point that uses this stub
     * @see BillingServiceGrpc.BillingServiceBlockingStub gRPC generated stub interface
     */
    private final BillingServiceGrpc.BillingServiceBlockingStub blockingStub;

    /**
     * Constructs a gRPC client instance for billing service communication.
     *
     * @param serverAddress The hostname or IP address of the billing service
     * @param serverPort    The gRPC server port of the billing service
     * @implNote Creates a managed channel with plaintext communication (use SSL/TLS in production)
     * @implWarning The usePlaintext() is suitable for local development but insecure for production
     */
    public BillingServiceGrpcClient(
            @Value("${billing.service.address:localhost}") String serverAddress,
            @Value("${billing.service.grpc.port:9001}") int serverPort) {

        log.info("Connecting to Billing Service GRPC service at {}:{}", serverAddress, serverPort);

        ManagedChannel channel = ManagedChannelBuilder.forAddress(serverAddress, serverPort).usePlaintext().build();

        blockingStub = BillingServiceGrpc.newBlockingStub(channel);
    }

    /**
     * Creates a new billing account through the gRPC service.
     *
     * @param patientId The unique identifier for the patient
     * @param name      The full name of the patient
     * @param email     The contact email for the billing account
     * @return BillingResponse containing the created account details
     * @throws io.grpc.StatusRuntimeException if the RPC fails
     * @implSpec Builds the protobuf request and uses the blocking stub for synchronous communication
     * @see BillingRequest
     * @see BillingResponse
     */
    public BillingResponse createBillingAccount(String patientId, String name, String email) {

        BillingRequest request = BillingRequest.newBuilder().setPatientId(patientId).setName(name).setEmail(email).build();

        BillingResponse response = blockingStub.createBillingAccount(request);
        log.info("Received response from billing service via GRPC: {}", response);
        return response;
    }
}