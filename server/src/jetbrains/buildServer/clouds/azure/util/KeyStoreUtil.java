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

import com.microsoft.windowsazure.core.utils.Base64;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.KeyStore;
import java.security.Security;

/**
 * Created by Maarten on 6/12/2014.
 */
public class KeyStoreUtil {
  /* Used to create the PKCS#12 store - important to note that the store is created on the fly so is in fact passwordless -
   * the JSSE fails with masqueraded exceptions so the BC provider is used instead - since the PKCS#12 import structure does
   * not have a password it has to be done this way otherwise BC can be used to load the cert into a keystore in advance and
   * password
   */
  public KeyStore createKeyStorePKCS12(String base64Certificate) throws Exception    {
    Security.addProvider(new BouncyCastleProvider());
    KeyStore store = KeyStore.getInstance("PKCS12", BouncyCastleProvider.PROVIDER_NAME);
    store.load(null, null);

    // read in the value of the base 64 cert without a password (PBE can be applied afterwards if this is needed
    InputStream sslInputStream = new ByteArrayInputStream(Base64.decode(base64Certificate));
    store.load(sslInputStream, "".toCharArray());

    // we need to a create a physical keystore as well here
    OutputStream out = new ByteArrayOutputStream();
    store.store(out, "".toCharArray());
    out.close();
    return store;
  }

  /* Used to get an SSL factory from the keystore on the fly - this is then used in the
   * request to the service management which will match the .publishsettings imported
   * certificate
   */
  private SSLSocketFactory getFactory(String base64Certificate) throws Exception  {
    KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
    KeyStore keyStore = createKeyStorePKCS12(base64Certificate);

    // gets the TLS context so that it can use client certs attached to the
    SSLContext context = SSLContext.getInstance("TLS");
    keyManagerFactory.init(keyStore, "".toCharArray());
    context.init(keyManagerFactory.getKeyManagers(), null, null);

    return context.getSocketFactory();
  }
}