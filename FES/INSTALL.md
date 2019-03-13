# FrontEndServer
This is a Spring STS project of the front-end server developed for the "Bonvoyage" European Project (http://bonvoyage2020.eu/).

## Installation Guide

##Prerequisites

- Install the NDN Forwarding Daemon and its requirements <https://github.com/named-data/NFD>
- Install MySql <https://www.mysql.it/downloads/>
- Install, configure and exucute the AUCSerive. You can make a git clone form: <https://github.com/OpenGeoBase/AUCService.git>
- If you don't have a valid ssl certificate on the server that runs that application, you can generate a self-signed one using this command

```shell
keytool -genkey -alias tomcat -storetype PKCS12 -keyalg RSA -keysize 2048 -keystore keystore.p12 -validity 3650
```

Specify a certificate name and password that you should use to configure the 'server.ssl.key-store' and 'server.ssl.key-store-password' fields into the 'application.properties' file.


## Installation Guide For Spring STS

- Open Spring STS
- Choose from menu File->Import
    - From Maven folder choose Existing Maven Project
    - Select the root folder of the project, than tap on next button and finish button
    - Open the project from the navigation on left side of the Spring STS
    - Open the folder "src/main/resources" and edit the application.properties file for changing the fields you needs
        - Edit the working server http and https ports
        - Edit the IP address and port of the Authentication Service. Example: fes.auc.url=http://192.168.58.100:8090/AUC
        - Edit the IP address of the NFD node. Example: fes.nfd.ip=127.0.0.1


## Build And Run Guide For Linux Command Line

- Open Folder and navigate to the root dir
- Type the maven build command

::
    mvn build

- Type the maven package command

::
    mvn package

 
