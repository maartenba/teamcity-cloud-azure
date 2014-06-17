/*
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jetbrains.buildServer.clouds.azure;

import jetbrains.buildServer.clouds.*;
import jetbrains.buildServer.clouds.azure.util.AzurePublishSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Created by Maarten on 6/12/2014.
 */
public class AzureCloudImage implements CloudImage {
  @NotNull
  private final String id;
  @NotNull
  private final String name;
  @NotNull
  private final Map<String, AzureCloudInstance> instances = new ConcurrentHashMap<String, AzureCloudInstance>();
  @Nullable
  private final CloudErrorInfo errorInfo;
  @NotNull
  private final ScheduledExecutorService executorService;
  private String azureSubscriptionId;
  @NotNull
  private AzurePublishSettings azurePublishSettings;
  private boolean isReusable = true;
  private String[] persistentVmNames;

  public AzureCloudImage(@NotNull final String imageId,
                         @NotNull final String imageName,
                         @NotNull final String subscriptionId,
                         @NotNull final AzurePublishSettings publishSettings,
                         @NotNull final String[] persistentVmNames,
                         @NotNull final ScheduledExecutorService executor) {
    id = imageId;
    name = imageName;
    azureSubscriptionId = subscriptionId;
    azurePublishSettings = publishSettings;
    this.persistentVmNames = persistentVmNames;
    executorService = executor;
    errorInfo = null;
    isReusable = true;

    populateMyInstances();
  }

  private void populateMyInstances() {
    for (String instanceId : persistentVmNames) {
      instances.put(instanceId, createInstance(instanceId));
    }
  }

  public boolean isReusable() {
    return isReusable;
  }

  @NotNull
  public String getId() {
    return id;
  }

  @NotNull
  public String getName() {
    return name;
  }

  @NotNull
  public Collection<? extends CloudInstance> getInstances() {
    return Collections.unmodifiableCollection(instances.values());
  }

  @Nullable
  public AzureCloudInstance findInstanceById(@NotNull final String instanceId) {
    return instances.get(instanceId);
  }

  @Nullable
  public CloudErrorInfo getErrorInfo() {
    return errorInfo;
  }

  @NotNull
  public synchronized AzureCloudInstance startNewInstance(@NotNull final CloudInstanceUserData data) {
    // check reusable instances
    for (AzureCloudInstance instance : instances.values()) {
      if (instance.getErrorInfo() == null && instance.getStatus() == InstanceStatus.STOPPED && instance.isRestartable()) {
        instance.start(data);
        return instance;
      }
    }

    return null;
  }

  protected AzureCloudInstance createInstance(String instanceId) {
    return new AzureCloudInstance(instanceId, azureSubscriptionId, azurePublishSettings, this, executorService);
  }

  void dispose() {
    for (final AzureCloudInstance instance : instances.values()) {
      instance.terminate();
    }
    instances.clear();
  }
}
