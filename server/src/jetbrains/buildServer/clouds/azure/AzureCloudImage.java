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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Created by Maarten on 6/12/2014.
 */
public class AzureCloudImage implements CloudImage {
  @NotNull private final String myId;
  @NotNull private final String myName;
  private String mySubscriptionId;
  @NotNull private AzurePublishSettings myPublishSettings;

  @NotNull private final Map<String, AzureCloudInstance> myInstances = new ConcurrentHashMap<String, AzureCloudInstance>();
  @Nullable private final CloudErrorInfo myErrorInfo;
  private boolean myIsReusable = true;
  private String[] myPersistentVmNames;
  @NotNull private final ScheduledExecutorService myExecutor;

  public AzureCloudImage(@NotNull final String imageId,
                         @NotNull final String subscriptionId,
                         @NotNull final AzurePublishSettings publishSettings,
                         @NotNull final String[] persistentVmNames,
                         @NotNull final ScheduledExecutorService executor) {
    myId = imageId;
    myName = imageId;
    mySubscriptionId = subscriptionId;
    myPublishSettings = publishSettings;
    myPersistentVmNames = persistentVmNames;
    myExecutor = executor;
    myErrorInfo = null;
    myIsReusable = true;

    populateMyInstances();
  }

  private void populateMyInstances() {
    for (String instanceId : myPersistentVmNames) {
      myInstances.put(instanceId, createInstance(instanceId));
    }
  }

  public boolean isReusable() {
    return myIsReusable;
  }

  @NotNull
  public String getId() {
    return myId;
  }

  @NotNull
  public String getName() {
    return myName;
  }

  @NotNull
  public Collection<? extends CloudInstance> getInstances() {
    return Collections.unmodifiableCollection(myInstances.values());
  }

  @Nullable
  public AzureCloudInstance findInstanceById(@NotNull final String instanceId) {
    return myInstances.get(instanceId);
  }

  @Nullable
  public CloudErrorInfo getErrorInfo() {
    return myErrorInfo;
  }

  @NotNull
  public synchronized AzureCloudInstance startNewInstance(@NotNull final CloudInstanceUserData data) {
    // check reusable instances
    for (AzureCloudInstance instance : myInstances.values()) {
      if (instance.getErrorInfo() == null && instance.getStatus() == InstanceStatus.STOPPED && instance.isRestartable()) {
        instance.start(data);
        return instance;
      }
    }

    return null;
  }

  protected AzureCloudInstance createInstance(String instanceId) {
    return new AzureCloudInstance(instanceId, mySubscriptionId, myPublishSettings, this, myExecutor);
  }

  void dispose() {
    for (final AzureCloudInstance instance : myInstances.values()) {
      instance.terminate();
    }
    myInstances.clear();
  }
}
