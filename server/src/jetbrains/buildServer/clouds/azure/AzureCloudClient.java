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
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Created by Maarten on 6/5/2014.
 */
public class AzureCloudClient extends BuildServerAdapter implements CloudClientEx {
  @NotNull private final List<AzureCloudImage> myImages = new ArrayList<AzureCloudImage>();
  @Nullable private CloudErrorInfo myErrorInfo;
  @NotNull private final ScheduledExecutorService myExecutor = Executors.newSingleThreadScheduledExecutor(new NamedDeamonThreadFactory("azure-cloud-image"));
  private final CloudClientParameters myCloudClientParams;
  private AzurePublishSettings myPublishSettings;

  public AzureCloudClient(CloudClientParameters cloudClientParams) {
    myCloudClientParams = cloudClientParams;

    // Parse publish settings
    String publishSettingsXml = cloudClientParams.getParameter(AzureCloudConstants.PARAM_NAME_PUBLISHSETTINGS);
    if (publishSettingsXml == null || publishSettingsXml.trim().length() == 0) {
      myErrorInfo = new CloudErrorInfo("No publish settings specified");
      return;
    }

    AzurePublishSettingsParser parser = new AzurePublishSettingsParser();
    try {
      myPublishSettings = parser.parse(publishSettingsXml, new FileOutputStream(AzureCloudConstants.getKeyStorePath()), AzureCloudConstants.KEYSTORE_PWD);
    } catch (Exception ex) {
      myErrorInfo = new CloudErrorInfo("Error while parsing publish settings: " + ex.getMessage());
      return;
    }

    String subscription = cloudClientParams.getParameter(AzureCloudConstants.PARAM_NAME_SUBSCRIPTION);
    if (subscription == null || subscription.trim().length() == 0) {
      myErrorInfo = new CloudErrorInfo("No subscription identifier specified");
      return;
    }

    // Parse VM names
    String vmNames = cloudClientParams.getParameter(AzureCloudConstants.PARAM_NAME_VMNAMES);
    if (vmNames == null || vmNames.trim().length() == 0) {
      myErrorInfo = new CloudErrorInfo("No VM names specified");
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
      AzureCloudImage image = new AzureCloudImage("Azure VMs", subscription, myPublishSettings, persistentVmNamesArray, myExecutor);
      myImages.add(image);
    }
  }

  @Nullable
  private AzureCloudImage findImage(@NotNull final AgentDescription agentDescription) {
    final String imageId = agentDescription.getConfigurationParameters().get(AzureCloudConstants.AGENT_PARAM_NAME_VMNAME);
    return imageId == null ? null : (AzureCloudImage)findImageById(imageId);
  }


  @Nullable
  private String findInstanceId(@NotNull final AgentDescription agentDescription) {
    return agentDescription.getConfigurationParameters().get(AzureCloudConstants.AGENT_PARAM_NAME_VMNAME);
  }

  @NotNull
  public CloudInstance startNewInstance(@NotNull CloudImage cloudImage, @NotNull CloudInstanceUserData cloudInstanceUserData) throws QuotaException {
    return ((AzureCloudImage)cloudImage).startNewInstance(cloudInstanceUserData);
  }

  public void restartInstance(@NotNull CloudInstance cloudInstance) {
    ((AzureCloudInstance)cloudInstance).restart();
  }

  public void terminateInstance(@NotNull CloudInstance cloudInstance) {
    ((AzureCloudInstance)cloudInstance).terminate();
  }

  public void dispose() {
    for (AzureCloudImage image : myImages) {
      image.dispose();
    }
    myImages.clear();
    myExecutor.shutdown();
  }

  public boolean isInitialized() {
    return true;
  }

  @Nullable
  public CloudImage findImageById(@NotNull String s) throws CloudException {
    for (final AzureCloudImage image : myImages) {
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
    return Collections.unmodifiableList(myImages);
  }

  @Nullable
  public CloudErrorInfo getErrorInfo() {
    return myErrorInfo;
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
