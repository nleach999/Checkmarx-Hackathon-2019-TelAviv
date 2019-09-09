# Checkmarx Plugin Updater

 _Documentation is currently a work in progress, more to come._

## Table of Contents
- [**Background**](#background) 
- [**Development Headstart**](#development-headstart)
- [**Deployment Methodology**](#deployment-methodology)
    - [**Locating a List of Plugin Payloads**](#locating-a-list-of-plugin-payloads)
    - [**Choosing Plugin Updates to Download**](#choosing-plugin-updates-to-download)
    - [**Installing the Plugin Update**](#installing-the-plugin-update)

## Background

When Checkmarx customers install a new release of SAST, developers that use IDE and CI plugins must update them to be compatible with the new release.  Some IDEs/CIs have an integrated "marketplace" that can notify plugin users when an update is available, but this is not universal across all of them.  The use of an IDE/CI marketplace, though efficient, may not be feasible for some organizations.  Large organizations, for example, may have policies requring approval before installing third-party components.  In some cases, access to public Internet may be restricted, minimizing the ability for the IDE/CI marketplace to phone home to detect available updates.

IDEs/CIs that do not have plugin updates available through the marketplace often require manual deployment of these updates. When there are thousands of developers, coordinating manual plugin updates across multiple IDEs/CIs can be difficult.  Even for teams with smaller numbers of developers, coordinating updates can be difficult when considering the entire development team may be geographically dispersed.

This project is part of the 2019 Checkmarx Hackathon in Tel Aviv, Israel.  The project goal is to:

* Design a deployment methodology compatible with both small and large development teams
* Create the logic libraries needed to enable IDE/CI plugin updates
* Show a proof-of-concept plugin in Eclipse that can perform the update


## Development Headstart

The ideal development IDE for this project is [Visual Studio Code](https://code.visualstudio.com/download).  Java, Git, and Docker plugins will allow you to automatically run unit tests and run the code in a debugger.

Executing `gradlew build` in the root of this project should properly build all components.

To see the logic in action, you need at least a test web server (one that runs locally is provided [here](test_webserver)) and to execute the [Command Line Tester](cmdline_tester).


## Deployment Methodology

### Locating a List of Plugin Payloads

To detect an update, each IDE running an instance of a plugin must first be able to find a list of potential update binary payloads.  This can be accomplished by the plugin update client code utilizing various host name resolution capabilities to resolve the name of a well-known host.  Most corporate networks utilize one or more name resoluton systems such as DNS, mDNS, or WINS.  

For development organizations that allow unrestricted Internet access and installation of 3rd-party components, this would be easy if Checkmarx hosted a well-known host serving plugin binary payloads.  For organizations with more restricted environments, particularly those that do not allow unrestricted Internet access, the use of a well-known name can be utilized to allow the organization to control the available plugins.

To locate the list of plugin binary payloads, name resolution starts with the well-known name "cxpluginupdate". An algorithm was created that somewhat follows the same technique used in [Web Proxy Auto Discovery (WPAD) protocol](https://en.wikipedia.org/wiki/Web_Proxy_Auto-Discovery_Protocol).  Name resolution is attempted in the following order:

1. cxpluginupdate
2. cxpluginupdate.local
3. The WPAD-like domain name expansion for domains assigned to all local network adapters

As an example of the domain name expansion, consider an adapter with the domain "mshome.microsoft.com".  The domain name expansion yields the following hosts:

* cxpluginupdate.mshome.microsoft.com
* cxpluginupdate.microsoft.com
* cxpluginupdate.com

An attempt will be made to connect to each host to obtain a list of available plugins.  The first host that responds is chosen as the source of the plugin update download.

The approach taken has the following benefits:

* There is no server-side code for Checkmarx to develop, update, and support.
* Organizations can stage plugin updates in advance of new versions of SAST being deployed.
* Organizations can monitor downloads through web server logs to gauge propagation of plugin updates across global development teams.
* The approach is compatible with supporting development teams of any size.


### Choosing Plugin Updates to Download

hosting multiple IDE plugins
plugin embeds regex to filter all plugin payloads other than the one that is an update to itself
versions are determined by lexical ordering of the filenames

The plugins are signed, preventing man-in-the-middle type attacks where a malicious plugin is injected into a resolveable host
the plugin can validate the signature is correct by embedding the public signing key
size checking before download prevents someone injecting an extremely large payload into the update server


### Installing the Plugin Update

Libraries detect updates and perform downloads.  Logic to install is left to the plugin code itself.












