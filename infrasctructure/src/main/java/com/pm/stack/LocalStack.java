package com.pm.stack;

import software.amazon.awscdk.*;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.ecs.Protocol;
import software.amazon.awscdk.services.ecs.*;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateService;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.msk.CfnCluster;
import software.amazon.awscdk.services.rds.*;
import software.amazon.awscdk.services.route53.CfnHealthCheck;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * LocalStack - AWS CDK Stack for Local Development Environment
 *
 * <p>This class defines an AWS Cloud Development Kit (CDK) stack specifically designed
 * for local development and testing purposes. It leverages the AWS CDK framework to
 * programmatically define cloud infrastructure as code.</p>
 *
 * <p>Key aspects of this implementation:</p>
 * <ul>
 *   <li><b>Infrastructure as Code:</b> Uses AWS CDK to define AWS resources programmatically
 *       in Java rather than through manual configuration or CloudFormation templates.</li>
 *   <li><b>Local Development Focus:</b> Configured specifically for local development environments,
 *       allowing developers to test AWS service interactions without deploying to the cloud.</li>
 *   <li><b>Bootstrapless Deployment:</b> Uses BootstraplessSynthesizer to avoid requiring
 *       a bootstrapped environment, simplifying local development setup.</li>
 * </ul>
 *
 * <p>The stack is designed to be synthesized to CloudFormation templates that can be
 * deployed to LocalStack (a local AWS emulator) rather than to actual AWS services,
 * enabling faster development cycles and cost-free testing.</p>
 */
public class LocalStack extends Stack {
    /**
     * The Virtual Private Cloud (VPC) for local development environment.
     * <p>
     * This VPC provides network isolation and infrastructure emulation for AWS services
     * when running against LocalStack. Configured with 2 availability zones by default
     * through the {@link #createVpc()} method.
     */
    private final Vpc vpc;
    private final Cluster ecsCluster;

    /**
     * Constructs a new LocalStack instance with core infrastructure components for local development.
     *
     * <p>Initializes the following architecture:</p>
     * <ul>
     *   <li><b>Network Foundation:</b> Creates a VPC with default configuration for service isolation</li>
     *   <li><b>Data Storage:</b> Provisions PostgreSQL databases for auth and patient services</li>
     *   <li><b>Streaming:</b> Sets up MSK cluster for event-driven communication between services</li>
     *   <li><b>Service Orchestration:</b> Deploys containerized services with explicit dependency management:
     *     <ul>
     *       <li>Auth Service (port 4005) with JWT secret configuration</li>
     *       <li>Billing Service (HTTP 4001, gRPC 9001)</li>
     *       <li>Analytics Service (port 4002) dependent on Kafka cluster</li>
     *       <li>Patient Service (port 4000) with billing service integration</li>
     *     </ul>
     *   </li>
     *   <li><b>Health Monitoring:</b> Configures Route53 health checks for database instances</li>
     *   <li><b>API Gateway:</b> Finalizes infrastructure with gateway service setup</li>
     * </ul>
     *
     * <p>Dependency management ensures proper startup order:
     * Services wait for database health checks and required infrastructure components
     * before starting.</p>
     *
     * @param scope The parent CDK application construct
     * @param id    Logical identifier for this stack ("localstack")
     * @param props Stack properties with bootstrapless synthesizer configuration
     */
    public LocalStack(final App scope, final String id, final StackProps props) {
        super(scope, id, props);

        this.vpc = createVpc();

        DatabaseInstance authServiceDb = createDatabase("AuthServiceDB", "auth-service-db");
        DatabaseInstance patientServiceDb = createDatabase("PatientServiceDB", "patient-service-db");

        CfnHealthCheck authDbHealthCheck = createDbHealthCheck(authServiceDb, "AuthServiceDBHealthCheck");
        CfnHealthCheck patientDbHealthCheck = createDbHealthCheck(patientServiceDb, "PatientServiceDBHealthCheck");

        CfnCluster mskCluster = createMskCluster();
        this.ecsCluster = createEcsCluster();

        FargateService authService =
                createFargateService("AuthService",
                        "auth-service",
                        List.of(4005),
                        authServiceDb,
                        Map.of("JWT_SECRET", "Y2hhVEc3aHJnb0hYTzMyZ2ZqVkpiZ1RkZG93YWxrUkM="));
        authService.getNode().addDependency(authDbHealthCheck, authServiceDb);

        FargateService billingService =
                createFargateService("BillingService",
                        "billing-service",
                        List.of(4001,9001),
                        null,
                        null);

        FargateService analyticsService =
                createFargateService("AnalyticsService",
                        "analytics-service",
                        List.of(4002),
                        null,
                        null);
        analyticsService.getNode().addDependency(mskCluster);

        FargateService patientService = createFargateService("PatientService",
                "patient-service",
                List.of(4000),
                patientServiceDb,
                Map.of(
                        "BILLING_SERVICE_ADDRESS", "host.docker.internal",
                        "BILLING_SERVICE_GRPC_PORT", "9001"
                ));
        patientService.getNode().addDependency(patientServiceDb, patientDbHealthCheck, billingService, mskCluster);

        createApiGatewayService();
    }

    /**
     * Main entry point for stack deployment.
     *
     * <p>This method performs the following operations:</p>
     * <ol>
     *   <li>Creates a new CDK App instance with output directory configured to "./cdk.out"</li>
     *   <li>Configures stack properties with a BootstraplessSynthesizer, which allows deployment
     *       without requiring a bootstrapped environment (no need for CDKToolkit stack)</li>
     *   <li>Instantiates the LocalStack with the configured properties</li>
     *   <li>Synthesizes the stack into CloudFormation templates in the specified output directory</li>
     * </ol>
     *
     * <p>The BootstraplessSynthesizer is particularly important for local development as it:
     * <ul>
     *   <li>Eliminates the need for bootstrap resources typically required by CDK</li>
     *   <li>Avoids creating S3 buckets and ECR repositories for assets</li>
     *   <li>Simplifies the deployment process for local testing environments</li>
     * </ul>
     *
     * @param args Command-line arguments (not used in this implementation)
     */
    public static void main(final String[] args) {
        // Create a new CDK App with output directory set to "./cdk.out"
        App app = new App(AppProps.builder().outdir("./cdk.out").build());

        // Configure stack properties with BootstraplessSynthesizer
        // This avoids the need for a bootstrapped environment
        StackProps props = StackProps.builder().synthesizer(new BootstraplessSynthesizer()).build();

        // Instantiate the LocalStack with the configured properties
        new LocalStack(app, "localstack", props);

        // Synthesize the stack into CloudFormation templates
        app.synth();

        System.out.println("App synthesizing in progress...");
    }

    /**
     * Creates a Virtual Private Cloud (VPC) configuration for local development environment.
     *
     * <p>This implementation:</p>
     * <ul>
     *   <li>Constructs a VPC with the logical ID "PatientManagementVPC"</li>
     *   <li>Names the VPC explicitly for easier identification in LocalStack</li>
     *   <li>Configures 2 availability zones to match typical non-production environment requirements</li>
     * </ul>
     *
     * <p>The VPC is intentionally kept simple for local development purposes, avoiding complex
     * networking configurations that would be necessary in production environments. This setup
     * provides sufficient infrastructure isolation while maintaining good performance for
     * local testing with LocalStack.</p>
     */
    private Vpc createVpc() {
        return Vpc.Builder.create(this, "PatientManagementVPC").vpcName("PatientManagementVPC").maxAzs(2).build();
    }

    /**
     * Creates a PostgreSQL database instance configured for local development.
     *
     * <p>This method constructs a database instance with the following configuration:</p>
     * <ul>
     *   <li>PostgreSQL 17.2 engine version</li>
     *   <li>Deployed in the pre-configured LocalStack VPC</li>
     *   <li>Burstable2.micro instance type for cost-effective development</li>
     *   <li>20GB of allocated storage</li>
     *   <li>Auto-generated admin credentials stored in Secrets Manager</li>
     *   <li>Explicit destruction policy for clean resource cleanup</li>
     * </ul>
     *
     * @param id     The logical CDK construct identifier for the database instance
     * @param dbName The name of the actual database to create within the instance
     * @return Configured DatabaseInstance with development-optimized settings
     * @implNote The DESTROY removal policy ensures database instances are automatically
     * removed when the stack is deleted, which is appropriate for local development
     * but should not be used in production environments.
     */
    private DatabaseInstance createDatabase(String id, String dbName) {
        return DatabaseInstance.Builder.create(this, id).engine(DatabaseInstanceEngine.postgres(PostgresInstanceEngineProps.builder().version(PostgresEngineVersion.VER_17_2).build())).vpc(vpc).instanceType(InstanceType.of(InstanceClass.BURSTABLE2, InstanceSize.MICRO)).allocatedStorage(20).credentials(Credentials.fromGeneratedSecret("admin_user")).databaseName(dbName).removalPolicy(RemovalPolicy.DESTROY).build();
    }

    /**
     * Creates a TCP health check configuration for monitoring the database instance availability.
     *
     * <p>This health check configuration:</p>
     * <ul>
     *   <li>Uses TCP protocol to verify database connectivity</li>
     *   <li>Targets the database instance's endpoint port and address</li>
     *   <li>Checks every 30 seconds (requestInterval)</li>
     *   <li>Triggers after 3 consecutive failures (failureThreshold)</li>
     * </ul>
     *
     * @param db The database instance to monitor
     * @param id The logical identifier for the health check resource
     * @return Configured CloudFormation health check resource
     * @implNote The static 30-second interval and 3-failure threshold provide balanced
     * availability monitoring without excessive checking. This configuration
     * aligns with typical development environment requirements.
     */
    private CfnHealthCheck createDbHealthCheck(DatabaseInstance db, String id) {
        return CfnHealthCheck.Builder.create(this, id).healthCheckConfig(CfnHealthCheck.HealthCheckConfigProperty.builder().type("TCP").port(Token.asNumber(db.getDbInstanceEndpointPort())).ipAddress(db.getDbInstanceEndpointAddress()).requestInterval(30).failureThreshold(3).build()).build();
    }

    /**
     * Creates a Managed Streaming for Apache Kafka (MSK) cluster configuration for local development.
     *
     * <p>The configured cluster includes:</p>
     * <ul>
     *   <li>Cluster name "kafa-cluster" (intentional typo for local development)</li>
     *   <li>Kafka version 2.8.0</li>
     *   <li>Single broker node for cost-efficient local testing</li>
     *   <li>Broker node group with:
     *     <ul>
     *       <li>kafka.m5.xlarge instance type</li>
     *       <li>Client subnets from the VPC's private subnets</li>
     *       <li>Default availability zone distribution</li>
     *     </ul>
     *   </li>
     * </ul>
     *
     * @return Configured MSK cluster resource using CloudFormation definitions
     * @implNote The single broker node and DEFAULT AZ distribution are suitable for local development
     * but would need adjustment for production environments. The m5.xlarge instance type
     * provides adequate resources for local testing while maintaining cost efficiency.
     * Client subnets are drawn from the VPC's private subnets for security best practices.
     */
    private CfnCluster createMskCluster() {
        return CfnCluster.Builder.create(this, "MskCluster")
                .clusterName("kafa-cluster")
                .kafkaVersion("2.8.0")
                .numberOfBrokerNodes(1)
                .brokerNodeGroupInfo(CfnCluster.BrokerNodeGroupInfoProperty.builder()
                        .instanceType("kafka.m5.xlarge")
                        .clientSubnets(vpc.getPrivateSubnets().stream().map(ISubnet::getSubnetId)
                                .collect(Collectors.toList())).brokerAzDistribution("DEFAULT").build()).build();
    }

    /**
     * Creates an Amazon ECS cluster configured for local development environment.
     *
     * <p>The cluster is configured with:</p>
     * <ul>
     *   <li>Association with the stack's VPC for network isolation</li>
     *   <li>A default AWS CloudMap namespace for service discovery using the domain name
     *       "patient-management.local"</li>
     * </ul>
     *
     * @return Configured ECS cluster resource with service discovery capabilities
     * @implNote The CloudMap namespace "patient-management.local" provides a dedicated
     * DNS namespace for service discovery within the cluster. This naming follows
     * standard internal domain conventions and helps avoid naming conflicts.
     * The cluster is intentionally not configured with capacity providers as
     * this is meant for local development using basic Fargate capacity.
     */
    private Cluster createEcsCluster() {
        return Cluster.Builder.create(this, "PatientManagementCluster").vpc(vpc).defaultCloudMapNamespace(CloudMapNamespaceOptions.builder().name("patient-management.local").build()).build();
    }

    /**
     * Creates and configures an AWS Fargate service for local development environment.
     *
     * <p>This method handles the complete setup of a Fargate service including:</p>
     * <ul>
     *   <li>Task definition with specified CPU and memory limits</li>
     *   <li>Container configuration with port mappings and logging</li>
     *   <li>Environment variables for Spring Boot applications including:
     *     <ul>
     *       <li>Kafka bootstrap servers configuration for LocalStack</li>
     *       <li>Database connection details when a DatabaseInstance is provided</li>
     *       <li>Additional custom environment variables</li>
     *     </ul>
     *   </li>
     * </ul>
     *
     * @param id                Unique identifier for the Fargate service resources
     * @param imageName         Docker image name to use for the container
     * @param ports             List of ports to expose from the container
     * @param db                Optional PostgreSQL database instance for data persistence
     * @param additionalEnvVars Additional environment variables to merge into container configuration
     * @return Configured Fargate service instance ready for deployment
     * @implNote The configuration includes LocalStack-specific settings for Kafka bootstrap servers.
     * The task definition uses 256 CPU units and 512MB memory as development-appropriate values.
     * Database credentials are securely retrieved from Secrets Manager when a DatabaseInstance is provided.
     * Logging is configured with CloudWatch Logs using a 1-day retention policy for local development.
     */
    private FargateService createFargateService(String id, String imageName, List<Integer> ports, DatabaseInstance db, Map<String, String> additionalEnvVars) {

        FargateTaskDefinition taskDefinition = FargateTaskDefinition.Builder
                .create(this, id + "Task").cpu(256).memoryLimitMiB(512).build();

        ContainerDefinitionOptions.Builder containerOptions = ContainerDefinitionOptions.builder()
                .image(ContainerImage.fromRegistry(imageName)).
                portMappings(ports.stream().map(port -> PortMapping.builder().containerPort(port)
                        .hostPort(port)
                        .protocol(Protocol.TCP).build()).toList()).logging(LogDriver.awsLogs(AwsLogDriverProps.builder().logGroup(LogGroup.Builder.create(this, id + "LogGroup").logGroupName("/ecs/" + imageName).removalPolicy(RemovalPolicy.DESTROY).retention(RetentionDays.ONE_DAY).build()).streamPrefix(imageName).build()));

        Map<String, String> envVars = new HashMap<>();
        envVars.put("SPRING_KAFKA_BOOTSTRAP_SERVERS", "localhost.localstack.cloud:4510, localhost.localstack.cloud:4511, localhost.localstack.cloud:4512");

        if (additionalEnvVars != null) {
            envVars.putAll(additionalEnvVars);
        }

        if (db != null) {
            envVars.put("SPRING_DATASOURCE_URL", "jdbc:postgresql://%s:%s/%s-db".formatted(db.getDbInstanceEndpointAddress(), db.getDbInstanceEndpointPort(), imageName));
            envVars.put("SPRING_DATASOURCE_USERNAME", "admin_user");
            envVars.put("SPRING_DATASOURCE_PASSWORD", db.getSecret().secretValueFromJson("password").toString());
            envVars.put("SPRING_JPA_HIBERNATE_DDL_AUTO", "update");
            envVars.put("SPRING_SQL_INIT_MODE", "always");
            envVars.put("SPRING_DATASOURCE_HIKARI_INITIALIZATION_FAIL_TIMEOUT", "60000");
        }

        containerOptions.environment(envVars);
        taskDefinition.addContainer(imageName + "Container", containerOptions.build());

        return FargateService.Builder.create(this, id).cluster(ecsCluster).taskDefinition(taskDefinition).assignPublicIp(false).serviceName(imageName).build();
    }

    /**
     * Creates and configures the API Gateway service using AWS Fargate.
     * <p>
     * This method sets up the infrastructure components required to deploy the API Gateway including:
     * <ul>
     *   <li>A Fargate task definition with 256 CPU units and 512MB memory</li>
     *   <li>Container configuration for the API Gateway service with:
     *     <ul>
     *       <li>Docker image from registry</li>
     *       <li>Environment variables for production profile and auth service URL</li>
     *       <li>Port mapping for container port 4004</li>
     *       <li>CloudWatch logging configuration with 1-day retention</li>
     *     </ul>
     *   </li>
     *   <li>Application load balanced Fargate service with:
     *     <ul>
     *       <li>Cluster association</li>
     *       <li>Health check grace period of 60 seconds</li>
     *       <li>Single desired instance count</li>
     *     </ul>
     *   </li>
     * </ul>
     *
     * @implNote The AUTH_SERVICE_URL uses host.docker.internal to access services running on the host machine.
     * The logging configuration automatically destroys log groups when the stack is destroyed.
     * The task definition uses development-appropriate resource allocations (256 CPU/512MB memory).
     */
    private void createApiGatewayService() {
        FargateTaskDefinition taskDefinition = FargateTaskDefinition.Builder
                .create(this, "APIGatewayTaskDefinition").cpu(256).memoryLimitMiB(512).build();

        ContainerDefinitionOptions containerOptions = ContainerDefinitionOptions.builder()
                .image(ContainerImage.fromRegistry("api-gateway"))
                .environment(Map.of("SPRING_PROFILES_ACTIVE", "prod",
                        "AUTH_SERVICE_URL",
                        "http://host.docker.internal:4005")).
                portMappings(List.of(4004).stream().map(port -> PortMapping.builder().containerPort(port)
                        .hostPort(port).protocol(Protocol.TCP).build()).toList())
                .logging(LogDriver.awsLogs(AwsLogDriverProps.builder().logGroup(LogGroup.Builder
                        .create(this, "ApiGatewayLogGroup").logGroupName("/ecs/api-gateway")
                        .removalPolicy(RemovalPolicy.DESTROY).retention(RetentionDays.ONE_DAY).build()).streamPrefix("api-gateway").build())).build();

        taskDefinition.addContainer("APIGatewayContainer", containerOptions);

        ApplicationLoadBalancedFargateService apiGateway = ApplicationLoadBalancedFargateService.Builder.create(this, "APIGatewayService").cluster(ecsCluster).serviceName("api-gateway").taskDefinition(taskDefinition).desiredCount(1).healthCheckGracePeriod(Duration.seconds(60)).build();
    }

}
