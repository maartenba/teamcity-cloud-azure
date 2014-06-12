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

import jetbrains.buildServer.clouds.CloudClientFactory;
import jetbrains.buildServer.clouds.CloudClientParameters;
import jetbrains.buildServer.clouds.CloudRegistrar;
import jetbrains.buildServer.clouds.CloudState;
import jetbrains.buildServer.serverSide.AgentDescription;
import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.PropertiesProcessor;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * Created by Maarten on 6/5/2014.
 */
public class AzureCloudClientFactory implements CloudClientFactory {
  @NotNull private final String myEditProfileJspPath;

  public AzureCloudClientFactory(@NotNull final CloudRegistrar cloudRegistrar,
                                 @NotNull final PluginDescriptor pluginDescriptor) {
    myEditProfileJspPath = pluginDescriptor.getPluginResourcesPath("profile-settings.jsp");
    cloudRegistrar.registerCloudFactory(this);
  }

  @NotNull
  public AzureCloudClient createNewClient(@NotNull final CloudState state, @NotNull final CloudClientParameters params) {
    return new AzureCloudClient(params);
  }


  @NotNull
  public String getCloudCode() {
    return AzureCloudConstants.CLOUD_CODE;
  }

  @NotNull
  public String getDisplayName() {
    return AzureCloudConstants.CLOUD_DISPLAY_NAME;
  }

  public String getEditProfileUrl() {
    return myEditProfileJspPath;
  }

  @NotNull
  public Map<String, String> getInitialParameterValues() {
    return Collections.emptyMap();
  }

  @NotNull
  public PropertiesProcessor getPropertiesProcessor() {
    return new PropertiesProcessor() {
      public Collection<InvalidProperty> process(final Map<String, String> properties) {
        return Collections.emptyList();
      }
    };
  }

  public boolean canBeAgentOfType(@NotNull final AgentDescription description) {
    final Map<String, String> ps = description.getDefinedParameters();
    return ps.containsKey(AzureCloudConstants.PARAM_NAME_PUBLISHSETTINGS)
            && ps.containsKey(AzureCloudConstants.PARAM_NAME_SUBSCRIPTION)
            && ps.containsKey(AzureCloudConstants.PARAM_NAME_VMNAMES);
  }
}
