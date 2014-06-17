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

import com.intellij.openapi.util.text.StringUtil;
import jetbrains.buildServer.clouds.*;
import jetbrains.buildServer.clouds.azure.util.AzurePublishSettings;
import jetbrains.buildServer.clouds.azure.util.AzurePublishSettingsParser;
import jetbrains.buildServer.serverSide.AgentDescription;
import jetbrains.buildServer.serverSide.BuildServerAdapter;
import jetbrains.buildServer.util.NamedDeamonThreadFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.FileOutputStream;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class AzureCloudClient extends BuildServerAdapter implements CloudClientEx {
  @NotNull
  private final List<AzureCloudImage> cloudImages = new ArrayList<AzureCloudImage>();
  @NotNull
  private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor(new NamedDeamonThreadFactory("azure-cloud-image"));
  private final CloudClientParameters cloudClientParameters;
  @Nullable
  private CloudErrorInfo errorInfo;

  public AzureCloudClient(CloudClientParameters params) {
    cloudClientParameters = params;

    // Parse publish settings
    String publishSettingsXml = cloudClientParameters.getParameter(AzureCloudConstants.PARAM_NAME_PUBLISHSETTINGS);
    if (publishSettingsXml == null || publishSettingsXml.trim().length() == 0) {
      errorInfo = new CloudErrorInfo("No publish settings specified");
      return;
    }

    AzurePublishSettingsParser parser = new AzurePublishSettingsParser();
    AzurePublishSettings publishSettings;
    try {
      publishSettings = parser.parse(publishSettingsXml, new FileOutputStream(AzureCloudConstants.getKeyStorePath()), AzureCloudConstants.KEYSTORE_PWD);
    } catch (Exception ex) {
      errorInfo = new CloudErrorInfo("Error while parsing publish settings: " + ex.getMessage());
      return;
    }

    String subscription = cloudClientParameters.getParameter(AzureCloudConstants.PARAM_NAME_SUBSCRIPTION);
    if (subscription == null || subscription.trim().length() == 0) {
      errorInfo = new CloudErrorInfo("No subscription identifier specified");
      return;
    }

    // Parse VM names
    String vmNames = cloudClientParameters.getParameter(AzureCloudConstants.PARAM_NAME_VMNAMES);
    if (vmNames == null || vmNames.trim().length() == 0) {
      errorInfo = new CloudErrorInfo("No VM names specified");
      return;
    }

    List<String> persistentVmNames = new ArrayList<String>();
    String[] allLines = StringUtil.splitByLines(vmNames.trim());
    for (String imageInfo : allLines) {
      imageInfo = imageInfo.trim();
      if (imageInfo.isEmpty() /*|| imageInfo.startsWith("@@")*/) continue;

      String imageName = imageInfo.trim();
      persistentVmNames.add(imageName);
    }

    if (!persistentVmNames.isEmpty()) {
      String[] persistentVmNamesArray = new String[persistentVmNames.size()];
      persistentVmNames.toArray(persistentVmNamesArray);
      AzureCloudImage image = new AzureCloudImage("reusable", "Azure VMs", subscription, publishSettings, persistentVmNamesArray, executorService);
      cloudImages.add(image);
    }
  }

  @Nullable
  private AzureCloudImage findImage(@NotNull final AgentDescription agentDescription) {
    // TODO: we probably want to do this based on another parameter
    // e.g. final String imageId = agentDescription.getConfigurationParameters().get("agent.name");
    final String imageId = "reusable";
    return imageId == null ? null : (AzureCloudImage)findImageById(imageId);
  }

  @Nullable
  private String findInstanceId(@NotNull final AgentDescription agentDescription) {
    // TODO: we probably want to do this based on another parameter
    return agentDescription.getDefinedParameters().get("system.agent.name");
  }

  @NotNull
  public CloudInstance startNewInstance(@NotNull CloudImage cloudImage, @NotNull CloudInstanceUserData cloudInstanceUserData) throws QuotaException {
    return ((AzureCloudImage) cloudImage).startNewInstance(cloudInstanceUserData);
  }

  public void restartInstance(@NotNull CloudInstance cloudInstance) {
    ((AzureCloudInstance) cloudInstance).restart();
  }

  public void terminateInstance(@NotNull CloudInstance cloudInstance) {
    ((AzureCloudInstance) cloudInstance).terminate();
  }

  public void dispose() {
    for (AzureCloudImage image : cloudImages) {
      image.dispose();
    }
    cloudImages.clear();
    executorService.shutdown();
  }

  public boolean isInitialized() {
    return true;
  }

  @Nullable
  public CloudImage findImageById(@NotNull String s) throws CloudException {
    for (final AzureCloudImage image : cloudImages) {
      if (image.getId().equals(s)) {
        return image;
      }
    }
    return null;

  }

  @Nullable
  public CloudInstance findInstanceByAgent(@NotNull AgentDescription agentDescription) {
    AzureCloudImage image = findImage(agentDescription);
    if (image == null) return null;


    final String instanceId = findInstanceId(agentDescription);
    if (instanceId == null) return null;


    return image.findInstanceById(instanceId);
  }

  @NotNull
  public Collection<? extends CloudImage> getImages() throws CloudException {
    return Collections.unmodifiableList(cloudImages);
  }

  @Nullable
  public CloudErrorInfo getErrorInfo() {
    return errorInfo;
  }

  public boolean canStartNewInstance(@NotNull CloudImage cloudImage) {
    return true;
  }

  @Nullable
  public String generateAgentName(@NotNull AgentDescription agentDescription) {
    AzureCloudImage image = findImage(agentDescription);
    if (image == null) return null;


    String instanceId = findInstanceId(agentDescription);
    if (instanceId == null) return null;

    return instanceId;
  }
}
