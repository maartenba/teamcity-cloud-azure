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

package jetbrains.buildServer.clouds.azure.util;

import java.security.KeyStore;
import java.util.List;

public class AzurePublishSettings {
  private String publishMethod;
  private String managementUrl;
  private KeyStore managementKeyStore;
  private List<AzureSubscription> subscriptions;

  public AzurePublishSettings(String publishMethod, String managementUrl, KeyStore managementKeyStore, List<AzureSubscription> subscriptions) {
    this.publishMethod = publishMethod;
    this.managementUrl = managementUrl;
    this.managementKeyStore = managementKeyStore;
    this.subscriptions = subscriptions;
  }

  public String getPublishMethod() {
    return publishMethod;
  }

  public void setPublishMethod(String publishMethod) {
    this.publishMethod = publishMethod;
  }

  public String getManagementUrl() {
    return managementUrl;
  }

  public void setManagementUrl(String managementUrl) {
    this.managementUrl = managementUrl;
  }

  public KeyStore getManagementKeyStore() {
    return managementKeyStore;
  }

  public void setManagementKeyStore(KeyStore managementKeyStore) {
    this.managementKeyStore = managementKeyStore;
  }

  public List<AzureSubscription> getSubscriptions() {
    return subscriptions;
  }

  public void setSubscriptions(List<AzureSubscription> subscriptions) {
    this.subscriptions = subscriptions;
  }
}