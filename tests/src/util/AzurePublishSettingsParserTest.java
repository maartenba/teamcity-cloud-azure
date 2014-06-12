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

package util;

import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.clouds.azure.util.AzurePublishSettings;
import jetbrains.buildServer.clouds.azure.util.AzurePublishSettingsParser;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.ByteArrayOutputStream;

/**
 * Created by Maarten on 6/12/2014.
 */
public class AzurePublishSettingsParserTest extends BaseTestCase {
  @Test
  public void Test() throws Exception {
    // arrange
    String publishSettingsXml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "<PublishData>\n" +
            "  <PublishProfile\n" +
            "    PublishMethod=\"AzureServiceManagementAPI\"\n" +
            "    Url=\"https://management.core.windows.net\"\n" +
            "    ManagementCertificate=\"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\">\n" +
            "    <Subscription\n" +
            "      Id=\"abc\"\n" +
            "      Name=\"def\" />\n" +
            "  </PublishProfile>\n" +
            "</PublishData>";

    // act
    AzurePublishSettingsParser parser = new AzurePublishSettingsParser();
    AzurePublishSettings result = parser.parse(publishSettingsXml, new ByteArrayOutputStream(), "");

    // assert
    Assert.assertEquals(result.getPublishMethod(), "AzureServiceManagementAPI");
    Assert.assertEquals(result.getManagementUrl(), "https://management.core.windows.net");
    Assert.assertNotNull(result.getManagementKeyStore());
    Assert.assertEquals(result.getSubscriptions().size(), 1);
    Assert.assertEquals(result.getSubscriptions().get(0).getId(), "abc");
    Assert.assertEquals(result.getSubscriptions().get(0).getName(), "def");
  }
}
