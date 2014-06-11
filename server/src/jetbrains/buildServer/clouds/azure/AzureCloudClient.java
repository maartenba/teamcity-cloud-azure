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
import jetbrains.buildServer.serverSide.AgentDescription;
import jetbrains.buildServer.serverSide.BuildServerAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Collection;

/**
 * Created by Maarten on 6/5/2014.
 */
public class AzureCloudClient extends BuildServerAdapter implements CloudClientEx {
  private CloudClientParameters myCloudClientParams;

  public AzureCloudClient(CloudClientParameters cloudClientParams) {
    myCloudClientParams = cloudClientParams;
  }

  @NotNull
  public CloudInstance startNewInstance(@NotNull CloudImage cloudImage, @NotNull CloudInstanceUserData cloudInstanceUserData) throws QuotaException {
    return null;
  }

  public void restartInstance(@NotNull CloudInstance cloudInstance) {

  }

  public void terminateInstance(@NotNull CloudInstance cloudInstance) {

  }

  public void dispose() {

  }

  public boolean isInitialized() {
    return false;
  }

  @Nullable
  public CloudImage findImageById(@NotNull String s) throws CloudException {
    return null;
  }

  @Nullable
  public CloudInstance findInstanceByAgent(@NotNull AgentDescription agentDescription) {
    return null;
  }

  @NotNull
  public Collection<? extends CloudImage> getImages() throws CloudException {
    return null;
  }

  @Nullable
  public CloudErrorInfo getErrorInfo() {
    return null;
  }

  public boolean canStartNewInstance(@NotNull CloudImage cloudImage) {
    return false;
  }

  @Nullable
  public String generateAgentName(@NotNull AgentDescription agentDescription) {
    return null;
  }
}
