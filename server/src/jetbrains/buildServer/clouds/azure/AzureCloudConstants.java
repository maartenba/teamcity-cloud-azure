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

import org.jetbrains.annotations.NotNull;

/**
 * Created by Maarten on 6/5/2014.
 */
public interface AzureCloudConstants {
  @NotNull String CLOUD_CODE = "azure";
  @NotNull String CLOUD_DISPLAY_NAME = "Microsoft Azure";
  @NotNull String FOO_PARAM_NAME = "cloud.azure.foo";
}

