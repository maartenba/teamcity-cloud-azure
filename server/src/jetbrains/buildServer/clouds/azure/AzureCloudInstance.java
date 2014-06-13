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

import com.microsoft.windowsazure.Configuration;
import com.microsoft.windowsazure.core.Builder;
import com.microsoft.windowsazure.core.utils.KeyStoreCredential;
import com.microsoft.windowsazure.core.utils.KeyStoreType;
import com.microsoft.windowsazure.credentials.CertificateCloudCredentials;
import com.microsoft.windowsazure.credentials.SubscriptionCloudCredentials;
import com.microsoft.windowsazure.management.compute.ComputeManagementClient;
import com.microsoft.windowsazure.management.compute.ComputeManagementClientImpl;
import com.microsoft.windowsazure.management.compute.ComputeManagementService;
import com.microsoft.windowsazure.management.compute.HostedServiceOperations;
import com.microsoft.windowsazure.management.compute.models.*;
import com.microsoft.windowsazure.management.configuration.ManagementConfiguration;
import jetbrains.buildServer.clouds.CloudErrorInfo;
import jetbrains.buildServer.clouds.CloudInstance;
import jetbrains.buildServer.clouds.CloudInstanceUserData;
import jetbrains.buildServer.clouds.InstanceStatus;
import jetbrains.buildServer.clouds.azure.util.AzurePublishSettings;
import jetbrains.buildServer.serverSide.AgentDescription;
import jetbrains.buildServer.util.ExceptionUtil;
import jetbrains.buildServer.util.FuncThrow;
import jetbrains.buildServer.util.Util;
import jetbrains.buildServer.util.WaitFor;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Created by Maarten on 6/12/2014.
 */
public class AzureCloudInstance implements CloudInstance {
  @NotNull
  private static final Logger LOG = Logger.getLogger(AzureCloudInstance.class);
  private static final int STATUS_WAITING_TIMEOUT = 15 * 60 * 1000;

  @NotNull
  private final String myId;
  private String mySubscriptionId;
  private AzurePublishSettings myPublishSettings;
  @NotNull
  private final AzureCloudImage myImage;
  @NotNull
  private final Date myStartDate;

  @NotNull
  private volatile InstanceStatus myStatus;
  @Nullable
  private volatile CloudErrorInfo myErrorInfo;

  @NotNull
  private final ScheduledExecutorService myAsync;

  public AzureCloudInstance(@NotNull final String instanceId, String subscriptionId, AzurePublishSettings publishSettings, @NotNull final AzureCloudImage image, @NotNull ScheduledExecutorService executor) {
    myId = instanceId;
    mySubscriptionId = subscriptionId;
    myPublishSettings = publishSettings;
    myImage = image;
    myStatus = InstanceStatus.STOPPED;
    myStartDate = new Date();
    myAsync = executor;
  }

  public boolean isRestartable() {
    return true;
  }

  @NotNull
  public String getInstanceId() {
    return myId;
  }

  @NotNull
  public String getName() {
    return myId;
  }

  @NotNull
  public String getImageId() {
    return myImage.getId();
  }

  @NotNull
  public AzureCloudImage getImage() {
    return myImage;
  }

  @NotNull
  public Date getStartedTime() {
    return myStartDate;
  }

  public String getNetworkIdentity() {
    return "cloud.azure." + getImageId();
  }

  @NotNull
  public InstanceStatus getStatus() {
    return myStatus;
  }

  @Nullable
  public CloudErrorInfo getErrorInfo() {
    return myErrorInfo;
  }

  public boolean containsAgent(@NotNull final AgentDescription agentDescription) {
    final Map<String, String> configParams = agentDescription.getConfigurationParameters();
    return myId.equals(configParams.get(AzureCloudConstants.AGENT_PARAM_NAME_VMNAME));
  }

  public void start(@NotNull final CloudInstanceUserData data) {
    myStatus = InstanceStatus.STARTING;

    myAsync.submit(ExceptionUtil.catchAll("Start Azure cloud instance: " + this, new StartAgentCommand(data)));
  }

  public void restart() {
    waitForStatus(InstanceStatus.RUNNING);
    myStatus = InstanceStatus.RESTARTING;
    try {
      doStop();
      waitForStatus(InstanceStatus.STOPPED);
      doStart();
    } catch (final Exception e) {
      processError(e);
    }
  }

  public void terminate() {
    try {
      doStop();
      cleanupStoppedInstance();
    } catch (final Exception e) {
      processError(e);
    }
  }

  protected void cleanupStoppedInstance() {
  }

  private void waitForStatus(@NotNull final InstanceStatus status) {
    new WaitFor(STATUS_WAITING_TIMEOUT) {
      @Override
      protected boolean condition() {
        return myStatus == status;
      }
    };
  }

  private void processError(@NotNull final Exception e) {
    final String message = e.getMessage();
    LOG.error(message, e);
    myErrorInfo = new CloudErrorInfo(message, message, e);
    myStatus = InstanceStatus.ERROR;
  }

  private void doStart() throws Exception {
    Util.doUnderContextClassLoader(getClass().getClassLoader(), new FuncThrow<Void, RuntimeException>() {
      public Void apply() {
        try {
          doStartInternal();
        } catch (Exception e) {
          e.printStackTrace();
        }
        return null;
      }
    });
  }

  private void doStartInternal() throws Exception {
    myStatus = InstanceStatus.STARTING;

    // TODO
    // AzureCloudInstance
    // LOG.info("Execution finished: " + execResult.getExitCode());

    ComputeManagementClient client = ComputeManagementService.create(ManagementConfiguration.configure(
            new URI(myPublishSettings.getManagementUrl()), mySubscriptionId, AzureCloudConstants.getKeyStorePath(), AzureCloudConstants.KEYSTORE_PWD, KeyStoreType.pkcs12));

    HostedServiceOperations hostedServicesOperations = client.getHostedServicesOperations();
    HostedServiceListResponse hostedServicesList = hostedServicesOperations.listAsync().get();
    for (HostedServiceListResponse.HostedService service : hostedServicesList.getHostedServices()) {
      String serviceName = service.getServiceName();
      HostedServiceGetDetailedResponse serviceDetails = hostedServicesOperations.getDetailedAsync(serviceName).get();

      List<HostedServiceGetDetailedResponse.Deployment> serviceDeployments = serviceDetails.getDeployments();
      if (!serviceDeployments.isEmpty()) {
        for (HostedServiceGetDetailedResponse.Deployment serviceDeployment : serviceDeployments) {
          for (Role role : serviceDeployment.getRoles()) {
            if (role.getRoleType().equalsIgnoreCase(VirtualMachineRoleType.PersistentVMRole.toString())) {
              for (RoleInstance instance : serviceDeployment.getRoleInstances()) {
                if (instance.getRoleName().equalsIgnoreCase(myId) && !instance.getInstanceStatus().equalsIgnoreCase(RoleInstanceStatus.READYROLE)) {
                  client.getVirtualMachinesOperations().startAsync(serviceName, serviceDeployment.getName(), instance.getInstanceName()).get();
                }
              }
            }
          }
        }
      }
    }

    myStatus = InstanceStatus.RUNNING;
  }

  private void doStop() throws Exception {
    Util.doUnderContextClassLoader(getClass().getClassLoader(), new FuncThrow<Void, RuntimeException>() {
      public Void apply() {
        try {
          doStopInternal();
        } catch (Exception e) {
          e.printStackTrace();
        }
        return null;
      }
    });
  }

  private void doStopInternal() throws Exception {
    myStatus = InstanceStatus.STOPPING;

    // TODO
    // LOG.info("Execution finished: " + execResult.getExitCode());


    ComputeManagementClient client = ComputeManagementService.create(ManagementConfiguration.configure(
            new URI(myPublishSettings.getManagementUrl()), mySubscriptionId, AzureCloudConstants.getKeyStorePath(), AzureCloudConstants.KEYSTORE_PWD, KeyStoreType.pkcs12));

    HostedServiceOperations hostedServicesOperations = client.getHostedServicesOperations();
    HostedServiceListResponse hostedServicesList = hostedServicesOperations.listAsync().get();
    for (HostedServiceListResponse.HostedService service : hostedServicesList.getHostedServices()) {
      String serviceName = service.getServiceName();
      HostedServiceGetDetailedResponse serviceDetails = hostedServicesOperations.getDetailedAsync(serviceName).get();

      List<HostedServiceGetDetailedResponse.Deployment> serviceDeployments = serviceDetails.getDeployments();
      if (!serviceDeployments.isEmpty()) {
        for (HostedServiceGetDetailedResponse.Deployment serviceDeployment : serviceDeployments) {
          for (Role role : serviceDeployment.getRoles()) {
            if (role.getRoleType().equalsIgnoreCase(VirtualMachineRoleType.PersistentVMRole.toString())) {
              for (RoleInstance instance : serviceDeployment.getRoleInstances()) {
                if (instance.getRoleName().equalsIgnoreCase(myId) && !instance.getInstanceStatus().equalsIgnoreCase(RoleInstanceStatus.STOPPEDVM)) {
                  VirtualMachineShutdownParameters params = new VirtualMachineShutdownParameters();
                  params.setPostShutdownAction(PostShutdownAction.StoppedDeallocated);
                  client.getVirtualMachinesOperations().shutdownAsync(serviceName, serviceDeployment.getName(), instance.getInstanceName(), params).get();
                }
              }
            }
          }
        }
      }
    }

    myStatus = InstanceStatus.STOPPED;
  }

  private class StartAgentCommand implements Runnable {
    private final CloudInstanceUserData myData;

    public StartAgentCommand(@NotNull final CloudInstanceUserData data) {
      myData = data;
    }

    public void run() {
      try {
        doStart();
      } catch (final Exception e) {
        processError(e);
      }
    }
  }
}