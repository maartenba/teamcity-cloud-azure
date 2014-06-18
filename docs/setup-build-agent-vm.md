# Setting up a build agent for your TeamCity server on Microsoft Azure

This document describes setting up a build agent for your TeamCity server on Microsoft Azure.

## Prerequisites

The following information will be needed before starting this guide:

* TeamCity server URL

## 1. Creating the Virtual Machine through the Windows Azure Portal

1. From the [Microsoft Azure Management Portal](http://manage.windowsazure.com), create a new virtual machine.
2. Select the virtual machine image required to build your projects.
3. Select virtual machine location, size and storage account details.
4. On the load balancer/firewall rules step, add a new rule "TeamCity Build Agent", forwarding port 9090 to port 9090.
5. Create and start the virtual machine.

## 2. Installing required software and SDK's

1. Open a Remote Desktop connection (or SSH session if you created a Linux machine) into the virtual machine.
2. Install the required software and SDK's to compile your projects.
3. Make sure to install Java JDK (JRE) 1.6+. The JVM should be available in the ```JAVA_HOME``` (```JRE_HOME```)  environment variable or be in the global path.
4. Make sure the virtual machine allows TCP traffic over port 9090.

## 3. Installing and configuring TeamCity build agent on the virtual machine

1. Install the TeamCity agent as described in [http://confluence.jetbrains.com/display/TCD8/Setting+up+and+Running+Additional+Build+Agents](http://confluence.jetbrains.com/display/TCD8/Setting+up+and+Running+Additional+Build+Agents).
2. Edit the ```<TeamCity Agent Home>/conf/buildagent.properties``` file and update the ```serverUrl``` value to the full URL of your TeamCity server.
3. Start the build agent and check if it connects to the TeamCity server. If this works, the virtual machine is ready for business.

## Agent authorization

When the agent name is configured differently than the default (hostname of the machine), the installed TeamCity agent must be authorized manually in the TeamCity server. If you want authorization to happen automatically, make sure the ```name``` property in ```<TeamCity Agent Home>/conf/buildagent.properties``` is set to the hostname of the virtual machine.