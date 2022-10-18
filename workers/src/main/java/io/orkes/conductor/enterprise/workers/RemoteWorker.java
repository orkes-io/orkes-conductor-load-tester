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

import com.netflix.conductor.client.config.PropertyFactory;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.TaskDef;

public abstract class RemoteWorker implements Worker {

    public int getRetryCount() {
        return PropertyFactory.getInteger(getTaskDefName(), "retryCount", 3);
    }

    public int getTimeoutSeconds() {
        return PropertyFactory.getInteger(getTaskDefName(), "timeoutSeconds", 3600);
    }

    public int getResponseTimeoutSeconds() {
        return PropertyFactory.getInteger(getTaskDefName(), "responseTimeoutSeconds", 600);
    }

    public TaskDef getTaskDef() {
        TaskDef taskDef = new TaskDef(getTaskDefName(), String.format("Remote %s worker", getTaskDefName()),
                getRetryCount(),
                getTimeoutSeconds());
        taskDef.setResponseTimeoutSeconds(getResponseTimeoutSeconds());
        taskDef.setOwnerEmail("orkes-workers@apps.orkes.io");
        return taskDef;
    }

    @Override
    public int getPollingInterval() {
        return 100;
    }
}
