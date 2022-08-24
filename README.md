# Corda Trading App
Open the project in Intellij:
- File -> Open -> trading-cordapp -> build.gradle -> OK -> Open as a Project

To debug PartyA node:
build nodes and run the party webservers -> Run | Edit Configurations -> + -> Remote JVM debug -> Port 5006 -> OK -> enter breakpoints in Flow/Contract/etc.java -> run the debugger

Run in terminal 1:
```
chmod +x gradlew (run only once)
./gradlew clean deployNodes
./build/nodes/runnodes --allow-hibernate-to-manage-app-schema
```
Run in terminal 2:
```
./gradlew runPartyAServer
```
Run in terminal 3:
```
./gradlew runPartyBServer
```

Run in terminal 4:
```
To install npm:
    - sudo apt-get install curl
    - curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.1/install.sh | bash
    - (close and reopen the terminal)
    - command -v nvm
    - nvm install --lts
    
To install UI packages (once):
    - cd clients/src/main/webapp
    - npm install 
    - npm install @material-ui/lab
    - npm install @mui/material @emotion/react @emotion/styled
    - npm install @mui/icons-material
    - npm install @mui/lab

To start the react server:
    - cd clients/src/main/webapp
    - npm start
```

If you change a .java file, you need to kill all the processes/servers (pkill -9 java) and rerun the commands shown above.

PartyA connects with webserver on port 10056.
PartyB connects with webserver on port 10057.


How to change a jar file in corda.jar (corda.jar is the platform for all cordapps). When running corda-network-bootstrapper, it will download corda.jar automatically. So after the nodes are generated, change the corda.jar to the jar we want to use. 

```
# Decompress corda.jar

jar -xvf corda.jar


# change BFT-SMaRt.jar to the one we want to use


# Compress the new jar

jar -cvf0m ../corda.jar ./META-INF/MANIFEST.MF .


```




How to run Jmeter:


To install jmeter:
create a directory: ```Corda-Test-Suite``` and unzip ```jmeter-corda-4.9.1-testsuite.zip``` here.
create a sub directoty as the deploy folder: ```extlibs```

To deploy workflows with jmeter:

- change the workflows: build.gradle file (include these dependencies in the jar):
```
    compile project(":contracts")
        // Token SDK dependencies.
    compile "$tokens_release_group:tokens-workflows:$tokens_release_version"
    compile "$tokens_release_group:tokens-contracts:$tokens_release_version"
    compile "$tokens_release_group:tokens-money:1.1"
 ```

- build the workflows jar

```
./gradlew workflows:build

```

- deploy the workflows jar (this task will copy workflows.jar to the jmeter deploy folder: ~Corda-Test-Suite/extlibs):

  ```./gradlew deploySampler```


Before build the trading application, change the workflows: build.gradle file to previous version (not include these dependencies in jar):
```   
cordaCompile "$tokens_release_group:tokens-workflows:$tokens_release_version"
cordaCompile "$tokens_release_group:tokens-contracts:$tokens_release_version"
cordaCompile "$tokens_release_group:tokens-money:1.1"
cordapp project(":contracts")

```

Then deploynodes as usual;

After all the nodes start up,

run the Jmeter in a new terminal:
```
    - cd ~/Corda-Test-Suite/

    - java -jar jmeter-corda.jar -XadditionalSearchPaths="./extlibs/" -XjmeterProperties ./jmeter.properties
```

Install Java on VM (once):
```
Run in /home/<username>:
$sudo apt update
$java -version (to check no jdk is installed)
$sudo apt-get install openjdk-8-jdk
$java -version (confirm the installation)
$nano ~/.bashrc
add the line: export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
Ctrl-O, Enter, Ctrl-X (to save and exit)
logout and login
$echo $JAVA_HOME (should be set as above)
```

How to deploy nodes on VM:
```
<username> : TestAdmin-1
<ip> : 22.111.111.222 (Azure) 
```

Run nodes:
```
Set up corda directory in VM:
(/home/<username>) $sudo -i (to become root user)
(/root) $sudo adduser --system --no-create-home --group corda (once)
(/root) $mkdir /opt/corda; chown corda:corda /opt/corda (once)

$./gradlew clean deployNodes in the project folder 
Copy build/nodes into another folder outside of the project folder.
Delete .cache, runnodes, .conf
Delete logs/ djvm/ drivers/ from every node's folder.

In nodes/ add config folder including hosts.config and system.config
In hosts.config change localhost to <ip>:
0 <ip> 11000 11001
1 <ip> 11010 11011
2 <ip> 11020 11021
3 <ip> 11030 11031

In every node and Notary folder change node.conf "localhost" to VM <ip> port:
p2pAddress="<ip>:10006"
rpcSettings {
    address="0.0.0.0:10016"
    adminAddress="0.0.0.0:10046"
}

In every Notary folder also change node.conf to:
clusterAddresses=
["<ip>:11000",
"<ip>:11010",
"<ip>:11020",
"<ip>:11030"]

The only files left in PartyA/ should be similar to:
|── PartyA
│   |── additional-node-infos
│   |── certificates
│   |── corda.jar
│   |── cordapps
│   |── network-parameters
│   |── node.conf
│   |── nodeInfo-E4477B559304AADFC0638772C0956A38FA2E2A7A5EB0E65D0D83E5884831879A
│   |── persistence.mv.db
│   └── persistence.trace.db
[do not delete persistence.mv.db perstistence.trace.db, cordapps/]

```

Move /nodes to VM:
```
$scp -r nodes/ <username>@<ip>:/home/<username>/
e.g.
$scp -r nodes/ ubuntu@43.131.24.141:/home/ubuntu/
$scp -r nodes/ TestAdmin-1@20.108.166.202:/home/TestAdmin-1/
```

Move /nodes to VM /opt/corda/:
```
(root/) $mv ../../home/<username>/nodes/* /opt/corda/
e.g.
(root/) $mv ../../home/ubuntu/nodes/* /opt/corda/
(root/) $mv ../../home/TestAdmin-1/nodes/* /opt/corda/
```

Run the nodes in VM in root/opt/corda (sudo -i):
```
$cd /opt/corda/PartyA/
$java -jar corda.jar --config-file=node.conf --base-directory=/opt/corda/PartyA/ --allow-hibernate-to-manage-app-schema 
$cd /opt/corda/NotaryService0/
$java -jar corda.jar run-migration-scripts --app-schemas
$java -jar corda.jar --config-file=node.conf --base-directory=/opt/corda/NotaryService0  --allow-hibernate-to-manage-app-schema

kill <process_ID> (to kill node) or pkill -9 java (to kill all)
```

Test that the node is running:
```
Download Corda Node Explorer (Beta release v0.1.1-rev) 
from https://docs.r3.com/en/platform/corda/4.8/open-source/node-explorer.html [not v0.1.2]
Node Hostname: <ip>
Node Port: rpcSettings.address (RPC connection address) (e.g. 10016 for PartyA)
RPC Username: <usr>
PRC Password: <pwd>
SSH Port: 22
SSH Username: <username>
SSH Password: <password>
```

To restart a node:

1. kill the node
2. delete these folders: ```rm -r artemis/ logs/ per*```
3. start the node again, and add the run-migration-script sub-command: ```java -jar corda.jar run-migration-scripts --core-schemas --app-schemas```

Network bootstrapper (preferred way to deploy nodes):  
```
0. In the project folder run ./gradlew clean deployNodes, to produce cordapps/ (find in build/nodes/PartyA/)
1. Download the 4.8.5 network bootstrapper .jar from https://software.r3.com/ui/native/corda-releases/net/corda/corda-tools-network-bootstrapper.
2. In a new directory called nodes/ put every node's .conf (from build/nodes/):
NotaryService0_node.conf
NotaryService1_node.conf
NotaryService2_node.conf
NotaryService3_node.conf
PartyA_node.conf 
PartyB_node.conf

NotaryService_node.conf should look like this: 
---------
devMode=true
myLegalName="O=Notary Service 0,L=Zurich,C=CH"
notary {
    bftSMaRt {
       clusterAddresses=[
                "127.0.0.1:11000",
                "127.0.0.1:11010",
                "127.0.0.1:11020",
                "127.0.0.1:11030"
       ]
            replicaId=0
    }
    serviceLegalName="O=BFT,L=Zurich,C=CH"
    validating=false
}
p2pAddress="<ip>:10009"
rpcSettings {
    address="0.0.0.0:10010"
    adminAddress="0.0.0.0:10110"
}
---------
3. Change the ports to the VM IP (if necessary)
4. Outside of nodes/ run:
java -jar corda-tools-network-bootstrapper-4.8.5.jar --dir nodes/
5. Delete .cache/ djvm/ logs/ from every node folder
6. Add config/ to every Notary
7. Add cordapps/ (from build/nodes/) to every node folder
8. scp nodes/ to the VM
```

Connect web servers running locally with nodes running on the VM:
```
In build.gradle (clients) change:
'--config.rpc.host=localhost'
to 
'--config.rpc.host=40.120.37.142'
Comment out jmeter dependencies in build.gradle (workflows) and all the samplers in jmeter/
Run the webservers and UI locally.
```
