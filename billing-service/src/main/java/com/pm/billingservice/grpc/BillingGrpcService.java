package com.pm.billingservice.grpc;

import billing.BillingRequest;
import billing.BillingResponse;
import billing.BillingServiceGrpc.BillingServiceImplBase;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Implementation of the gRPC billing service for handling billing account creation.
 * <p>
 * This service class extends the auto-generated gRPC service base class and implements
 * the unary RPC method defined in the protobuf service definition.
 * </p>
 *
 * @GrpcService annotation registers this implementation with the gRPC server
 * through Spring Boot autoconfiguration.
 */
@GrpcService
public class BillingGrpcService extends BillingServiceImplBase {
    private static final Logger log = LoggerFactory.getLogger(BillingGrpcService.class);

    /**
     * Implements the CreateBillingAccount RPC method.
     * <p>
     * This unary RPC endpoint receives a billing request and returns a billing response
     * with account details. The method follows the standard gRPC server-side pattern:
     * </p>
     * <ol>
     *   <li>Receive request object (auto-deserialized from protobuf)</li>
     *   <li>Process business logic</li>
     *   <li>Build and send response through StreamObserver</li>
     * </ol>
     *
     * @param request          The billing account creation request containing
     *                         client-provided parameters
     * @param responseObserver The response stream observer to handle the asynchronous
     *                         response delivery and completion notification
     * 
     * @see BillingServiceImplBase#createBillingAccount(BillingRequest, StreamObserver)
     * @see BillingRequest Protobuf request message definition
     * @see BillingResponse Protobuf response message definition
     */
    @Override
    public void createBillingAccount(BillingRequest request, StreamObserver<BillingResponse> responseObserver) {
        log.info("createBillingAccount request received {}", request.toString());

        // Business logic implementation would typically include:
        // - Input validation
        // - Database operations
        // - External service calls
        // - Transaction management
        // - Error handling
        
        BillingResponse response = BillingResponse.newBuilder()
                .setAccountId("12345")  // Example generated account ID
                .setStatus("ACTIVE")    // Initial account status
                .build();

        // Send single response back to client
        responseObserver.onNext(response);
        
        // Notify client that RPC is complete
        responseObserver.onCompleted();
    }
}
