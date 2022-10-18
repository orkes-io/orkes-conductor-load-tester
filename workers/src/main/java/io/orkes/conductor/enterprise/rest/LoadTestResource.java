/*
 * Copyright 2020 Orkes, Inc.
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
package io.orkes.conductor.enterprise.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.web.bind.annotation.*;

import com.netflix.conductor.common.metadata.workflow.StartWorkflowRequest;
import com.netflix.conductor.common.run.Workflow;

import io.orkes.conductor.client.WorkflowClient;
import io.orkes.conductor.common.model.WorkflowRun;

import com.google.common.base.Stopwatch;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("api/load")
@RequiredArgsConstructor
@Slf4j
public class LoadTestResource {

    private final WorkflowClient workflowClient;

    @PostMapping("/{workflowName}/async")
    @Operation(summary = "Get the list of pending tasks for a given task type")
    public WorkflowRun startWorkflow(@PathVariable("workflowName") String workflowName, @RequestBody Map<String, Object> result) {
        StartWorkflowRequest request = new StartWorkflowRequest();
        request.setName(workflowName);
        request.setInput(result);
        request.setVersion(1);
        request.setCorrelationId(UUID.randomUUID().toString());
        String workflowId = workflowClient.startWorkflow(request);
        try {
            WorkflowRun run = new WorkflowRun();
            run.setWorkflowId(workflowId);
            return run;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @PostMapping("/{workflowName}/sync")
    @Operation(summary = "Get the list of pending tasks for a given task type")
    public WorkflowRun executeWorkflow(@PathVariable("workflowName") String workflowName, @RequestBody Map<String, Object> result) {
        StartWorkflowRequest request = new StartWorkflowRequest();
        request.setName(workflowName);
        request.setInput(result);
        request.setVersion(1);
        request.setCorrelationId(UUID.randomUUID().toString());
        Stopwatch stopwatch = Stopwatch.createStarted();
        CompletableFuture<WorkflowRun> future = workflowClient.executeWorkflow(request, null);
        try {
            WorkflowRun run = future.get(5, TimeUnit.SECONDS);
            stopwatch.stop();
            log.info("sync request time for workflow {}, {}, status = {}", run.getWorkflowId(), stopwatch.elapsed(TimeUnit.MILLISECONDS), run.getStatus());
            return run;
        } catch (TimeoutException e) {
            List<Workflow> workflows = workflowClient.getWorkflows(workflowName, request.getCorrelationId(), true, true);
            if(!workflows.isEmpty()) {
                log.error("sync request timed out after 5 second. sending a workflow based on correlation id = {}", request.getCorrelationId());
                return toWorkflowRun(workflows.get(0));
            }
            log.error("sync request timed out after 5 second. nothing found based on correlation id = {}", request.getCorrelationId());
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static WorkflowRun toWorkflowRun(Workflow workflow) {
        WorkflowRun run = new WorkflowRun();

        run.setWorkflowId(workflow.getWorkflowId());
        run.setCorrelationId(workflow.getCorrelationId());
        run.setInput(workflow.getInput());
        run.setCreatedBy(workflow.getCreatedBy());
        run.setCreateTime(workflow.getCreateTime());
        run.setOutput(workflow.getOutput());
        run.setTasks(new ArrayList<>());
        workflow.getTasks().forEach(task -> run.getTasks().add(task));
        run.setPriority(workflow.getPriority());
        run.setUpdateTime(workflow.getUpdateTime());
        run.setStatus(Workflow.WorkflowStatus.valueOf(workflow.getStatus().name()));
        run.setVariables(workflow.getVariables());

        return run;
    }
}
