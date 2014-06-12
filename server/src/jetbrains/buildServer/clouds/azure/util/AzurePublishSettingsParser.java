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

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Maarten on 6/12/2014.
 */
public class AzurePublishSettingsParser {
  public AzurePublishSettings parse(String publishSettingsXml, OutputStream keyStoreOutputStream, String keystorePwd) throws Exception {
    return parse(new InputSource(new StringReader(publishSettingsXml)), keyStoreOutputStream, keystorePwd);
  }


  public AzurePublishSettings parse(InputSource publishSettings, OutputStream keyStoreOutputStream, String keystorePwd) throws Exception {
    Document publishSettingsDocument = createDocumentFromInputSource(publishSettings);

    // Get the PublishProfile
    NodeList ndPublishProfile = publishSettingsDocument.getElementsByTagName("PublishProfile");
    Element publishProfileElement = (Element)ndPublishProfile.item(0);

    // Get the PublishMethod
    String publishMethod = publishProfileElement.getAttribute("PublishMethod");

    // Get the management Url
    String managementUrl = publishProfileElement.getAttribute("Url");

    // Get the management KeyStore
    KeyStoreUtil keyStoreUtil = new KeyStoreUtil();
    String certificate = publishProfileElement.getAttribute("ManagementCertificate");
    KeyStore managementKeyStore = keyStoreUtil.createKeyStorePKCS12(certificate, keyStoreOutputStream, keystorePwd);

    // Get subscriptions
    List<AzureSubscription> subscriptions = new ArrayList<AzureSubscription>();
    NodeList ndSubscriptions = publishProfileElement.getElementsByTagName("Subscription");
    for (int i = 0; i < ndSubscriptions.getLength(); i++) {
      Element subscriptionElement = (Element)ndSubscriptions.item(i);
      AzureSubscription subscription = new AzureSubscription(
              subscriptionElement.getAttribute("Id"), subscriptionElement.getAttribute("Name"));
      subscriptions.add(subscription);
    }

    return new AzurePublishSettings(publishMethod, managementUrl, managementKeyStore, subscriptions);
  }

  protected Document createDocumentFromInputSource(InputSource publishSettings)
          throws ParserConfigurationException, SAXException, IOException {
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    DocumentBuilder db = dbf.newDocumentBuilder();
    Document publishSettingsDocument = db.parse(publishSettings);
    publishSettingsDocument.getDocumentElement().normalize();

    return publishSettingsDocument;
  }
}