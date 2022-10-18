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

import java.security.SecureRandom;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.RandomStringUtils;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import com.google.common.util.concurrent.Uninterruptibles;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LoadTestWorker extends RemoteWorker {
    private final String name;

    private final int waitTime;

    private final int pollingInterval;
    private int keyCount = 50;
    private SecureRandom secureRandom = new SecureRandom();

    public LoadTestWorker(String name, int waitTime, int pollingInterval) {
        this.name = name;
        this.waitTime = waitTime;
        this.pollingInterval = pollingInterval;
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
        return pollingInterval;
    }

    @Override
    public TaskResult execute(Task task) {

        if (waitTime > 0) {
            Uninterruptibles.sleepUninterruptibly(waitTime, TimeUnit.MILLISECONDS);
        }

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        int resultCount = Math.max(20, secureRandom.nextInt(keyCount));

        result.getOutputData().put("fixed", "hello");
        result.getOutputData().put("oddEven", "odd" + secureRandom.nextInt(2));
        result.getOutputData().put("thirds", "thirds" + secureRandom.nextInt(3));
        result.getOutputData().put("fourths", "fourths" + secureRandom.nextInt(4));
        result.getOutputData().put("fifths", "fifths" + secureRandom.nextInt(5));
        result.getOutputData().put("tenths", "tenths" + secureRandom.nextInt(10));

        result.getOutputData().put("randomNumber", resultCount);
        result.getOutputData().put("uuid1", UUID.randomUUID().toString());
        result.getOutputData().put("uuid2", UUID.randomUUID().toString());
        result.getOutputData().put("float", secureRandom.nextDouble());

        for (int i = 0; i < resultCount; i++) {
            result.getOutputData().put("key" + i, generateRandomString());
        }

        return result;
    }

}