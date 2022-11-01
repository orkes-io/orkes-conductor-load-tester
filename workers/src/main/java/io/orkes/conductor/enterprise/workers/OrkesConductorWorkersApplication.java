/*
 * Copyright 2022 Orkes, Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package io.orkes.conductor.enterprise.workers;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.commons.lang3.StringUtils;
import org.springdoc.core.customizers.OpenApiCustomiser;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.env.Environment;
import org.springframework.core.io.FileSystemResource;

import com.netflix.conductor.client.worker.Worker;

import io.orkes.conductor.client.*;
import io.orkes.conductor.client.automator.TaskRunnerConfigurer;

import io.swagger.v3.oas.models.servers.Server;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class, ValidationAutoConfiguration.class})
@ComponentScan(basePackages = {"io.orkes"})
public class OrkesConductorWorkersApplication {

    private static final String CONDUCTOR_SERVER_URL = "conductor.server.url";

    private static final String CONDUCTOR_GRPC_SERVER_URL = "conductor.grpc.server";

    private static final String CONDUCTOR_GRPC_SERVER_PORT = "conductor.grpc.port";

    private static final String CONDUCTOR_GRPC_SSL = "conductor.grpc.ssl";
    private static final String CONDUCTOR_CLIENT_KEY_ID = "conductor.security.client.key-id";
    private static final String CONDUCTOR_CLIENT_SECRET = "conductor.security.client.secret";

    private final Environment env;
    private final List<RemoteWorker> workersList;

    private final WorkflowClient workflowClient;

    private final TaskClient taskClient;

    private final MetadataClient metadataClient;

    private final ApiClient apiClient;

    private ScheduledExecutorService delayedTaskRegistrationService;

    public OrkesConductorWorkersApplication(Environment env, List<RemoteWorker> workersList) {
        this.env = env;
        this.workersList = workersList;
        this.apiClient = getApiClient();
        OrkesClients orkesClients = new OrkesClients(this.apiClient);
        this.workflowClient = orkesClients.getWorkflowClient();
        this.taskClient = orkesClients.getTaskClient();
        this.metadataClient = orkesClients.getMetadataClient();
    }

    public static void main(String[] args) throws IOException {
        log.info("Starting conductor enterprise workers ... v1");
        loadExternalConfig();
        SpringApplication.run(OrkesConductorWorkersApplication.class, args);
    }

    @Bean
    public TaskRunnerConfigurer taskRunnerConfigurer(List<Worker> workersList) {

        Map<String, Integer> taskThreadCount = new HashMap<>();
        workersList = new ArrayList<>();

        Integer threadCount = env.getProperty("conductor.worker.load_test.threads", Integer.class);

        Integer pollingInterval = env.getProperty("conductor.worker.load_test.pollInterval", Integer.class);
        if(pollingInterval == null) {
            pollingInterval = 50;
        }

        Integer pollCount = env.getProperty("conductor.worker.load_test.pollCount", Integer.class);
        if(pollCount == null) {
            pollCount = 20;
        }

        log.info("Starting workers with {} threads and {} ms polling interval", threadCount, pollingInterval);


        workersList.add(new LoadTestWorker("x_test_worker_0", 5_000, pollingInterval));
        taskThreadCount.put("x_test_worker_0", threadCount);

        workersList.add(new LoadTestWorker("x_test_worker_1", 4_000, pollingInterval));
        taskThreadCount.put("x_test_worker_1", threadCount);

        workersList.add(new LoadTestWorker("x_test_worker_2", 3_000, pollingInterval));
        taskThreadCount.put("x_test_worker_2", threadCount);

        workersList.add(new LoadTestWorker("x_test_worker_long_running", 10_000, pollingInterval));
        taskThreadCount.put("x_test_worker_long_running", threadCount);

        log.info("Starting workers : {}", workersList);
        TaskRunnerConfigurer runnerConfigurer = new TaskRunnerConfigurer
                .Builder(taskClient, workersList)
                .withTaskThreadCount(taskThreadCount)
                .withTaskPollTimeout(5)
                .withTaskPollCount(pollCount)
                .build();
        runnerConfigurer.init();
        return runnerConfigurer;
    }

    @Bean
    public OpenApiCustomiser openApiCustomiser(Environment environment) {
        List<Server> servers = new ArrayList<>();
        Server server = new Server();
        server.setDescription("Conductor Load Gen Server");
        server.setUrl(environment.getProperty("conductor.swagger.url"));
        servers.add(server);
        return openApi -> openApi.servers(servers).getPaths().values().stream()
                .flatMap(pathItem -> pathItem.readOperations().stream());
    }

    @Bean
    public WorkflowClient getWorkflowClient() {
        return workflowClient;
    }

    private ApiClient getApiClient() {
        String rootUri = env.getProperty(CONDUCTOR_SERVER_URL);
        String keyId = env.getProperty(CONDUCTOR_CLIENT_KEY_ID);
        String secret = env.getProperty(CONDUCTOR_CLIENT_SECRET);

        String grpcHost = env.getProperty(CONDUCTOR_GRPC_SERVER_URL);
        String grpcPort = env.getProperty(CONDUCTOR_GRPC_SERVER_PORT);
        boolean useSSL = Boolean.parseBoolean(env.getProperty(CONDUCTOR_GRPC_SSL));

        if(rootUri.endsWith("/")) {
            rootUri = rootUri.substring(0, rootUri.length()-1);
        }
        ApiClient apiClient = null;
        if(StringUtils.isNotBlank(keyId) && StringUtils.isNotBlank(secret)) {
            apiClient =  new ApiClient(rootUri, keyId, secret);
        } else {
            apiClient = new ApiClient(rootUri);
        }
        if(StringUtils.isNotBlank(grpcHost)) {
            int port = Integer.parseInt(grpcPort);
            apiClient.setUseGRPC(grpcHost, port);
            apiClient.setUseSSL(useSSL);
        }

        return apiClient;
    }

    private static void loadExternalConfig() throws IOException {
        String configFile = System.getProperty("CONDUCTOR_CONFIG_FILE");
        if (configFile == null) {
            configFile = System.getenv("CONDUCTOR_CONFIG_FILE");
        }
        if (!StringUtils.isEmpty(configFile)) {
            FileSystemResource resource = new FileSystemResource(configFile);
            if (resource.exists()) {
                System.getenv().forEach((k, v) -> {
                    log.info("System Env Props - Key: {}, Value: {}", k, v);
                    if (k.startsWith("conductor")) {
                        log.info("Setting env property to system property: {}", k);
                        System.setProperty(k, v);
                    }
                });
                Properties existingProperties = System.getProperties();
                existingProperties.forEach((k, v) -> log.info("Env Props - Key: {}, Value: {}", k, v));
                Properties properties = new Properties();
                properties.load(resource.getInputStream());
                properties.forEach((key, value) -> {
                    String keyString = (String) key;
                    if (existingProperties.getProperty(keyString) != null) {
                        log.info("Property : {} already exists with value: {}", keyString, value);
                    } else {
                        System.setProperty(keyString, (String) value);
                    }
                });
                log.info("Loaded {} properties from {}", properties.size(), configFile);
            } else {
                log.warn("Ignoring {} since it does not exist", configFile);
            }
        }
    }
}
