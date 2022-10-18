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

import java.util.Random;

import org.apache.commons.lang3.RandomStringUtils;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LongRunningWorker extends RemoteWorker {
    private final String name;

    public LongRunningWorker(String name) {
        this.name = name;
    }

    private static String generateRandomString() {
        Random random = new Random();
        int wordCount = Math.max(1, random.nextInt(5));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < wordCount; i++) {
            String rstring = RandomStringUtils.random(10, true, true);
            sb.append(" ");
            sb.append(rstring);
        }
        return sb.toString();
    }

    @Override
    public String getTaskDefName() {
        return name;
    }

    @Override
    public int getPollingInterval() {
        return 20;
    }

    @Override
    public TaskResult execute(Task task) {
        TaskResult result = new TaskResult(task);

        if(task.getPollCount() < 2) {
            result.setStatus(TaskResult.Status.IN_PROGRESS);
            result.setCallbackAfterSeconds(1);
        } else {
            result.setStatus(TaskResult.Status.COMPLETED);
        }

        return result;
    }

}