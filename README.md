# teamcity-cloud-azure

Microsoft Azure cloud plugin for TeamCity.

License: Apache 2.0

----------
**WARNING - The Microsoft Azure cloud plugin for TeamCity is still a very early alpha version. Use with caution!**

----------

## Plugin description

This plugin provides Microsoft Azure cloud support for TeamCity. By configuring a Microsoft Azure cloud in TeamCity, a set of known virtual build agents can be started and stopped on demand by the TeamCity server.

## Installation

* Download the plugin ZIP file [from the latest GitHub release](/releases)
* Copy it to the TeamCity plugins folder
* Restart TeamCity server and verify the plugin was installed from ***Administration | Plugins List***
* Create a new cloud profile from ***Administration | Agent Cloud*** and [enter the requested details](blob/master/docs/cloud-profile.md)

## Compatibility

Microsoft Azure cloud plugin for TeamCity has been tested with TeamCity 8.1.3.

## Features

* Start/stop existing Microsoft Azure build agents

## Roadmap/wishlist

* Create/destroy Microsoft Azure build agents based on an image
* Better looking Cloud Profile editing
* Migrate from Java to [Kotlin](http://kotlin.jetbrains.com)

## Known issues

* Only one Microsoft Azure cloud configuration can be created per TeamCity server because the ```KeyStore``` being configured by the plugin only stores one management certificate.
* Status of the VM displayed in TeamCity is not always current. The VM status is read from TeamCity's last known status, not from Microsoft Azure.
* There are a lot of unknown issues.

## Agent VM prerequisites

For every VM that will be started/stopped using the Microsoft Azure cloud plugin for TeamCity, the following prerequisites should be in place:

* The VM should have the TeamCity agent installed and started as a service.
* The TeamCity server and agent should be able to communicate, either over the public Internet or using a Microsoft Azure VNET. It may be necessary to open certain load balancer and/or firewall ports (e.g. the TeamCity agent port 9090).
* When the agent name is configured differently than the default (hostname ofhe machine), the installed TeamCity agent must be authorized by the TeamCity server.
