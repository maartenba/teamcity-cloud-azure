<%--
  ~ Copyright 2000-2014 JetBrains s.r.o.
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~ http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  --%>

<%@ page import="jetbrains.buildServer.clouds.azure.AzureCloudConstants" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<c:set var="publishSettingsLink"><a href="https://windows.azure.com/download/publishprofile.aspx" target="_blank">Download Azure publish settings</a></c:set>
<c:set var="paramPublishSettings" value="<%=AzureCloudConstants.PARAM_NAME_PUBLISHSETTINGS%>"/>
<c:set var="paramSubscription" value="<%=AzureCloudConstants.PARAM_NAME_SUBSCRIPTION%>"/>
<c:set var="paramVmNames" value="<%=AzureCloudConstants.PARAM_NAME_VMNAMES%>"/>

<script type="text/javascript">
  BS = BS || {};
  BS.Clouds = BS.Clouds || {};
  BS.Clouds.Azure = {};
</script>

<tr>
  <th><label for="secure:${paramPublishSettings}">Publish settings: <l:star/></label></th>
  <td><props:multilineProperty name="secure:${paramPublishSettings}" className="longField" linkTitle="Publish settings XML" cols="55" rows="5" expanded="${true}" />
    <span id="error_secure:${paramPublishSettings}" class="error"></span>
    <span class="smallNote">Your Azure Publish Settings. ${publishSettingsLink} and copy/paste the file contents in here.</span>
  </td>
</tr>

<tr>
  <th><label for="${paramSubscription}">Subscription: <l:star/></label></th>
  <td><props:textProperty name="${paramSubscription}" className="longField"/>
    <span id="error_${paramSubscription}" class="error"></span>
    <span class="smallNote">Subscription identifier in which the Virtual Machines will be located.</span>
  </td>
</tr>

<tr>
  <th><label for="${paramVmNames}">Agent VM names: <l:star/></label></th>
  <td>
    <props:multilineProperty name="${paramVmNames}" className="longField" linkTitle="Agent VMs to run" cols="55" rows="5" expanded="${true}"/>
    <span class="smallNote">List of agent VMs, each on a new line.</span>
  </td>
</tr>
