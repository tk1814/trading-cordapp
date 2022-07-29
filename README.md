# Corda Trading App
Open the project in Intellij:
- File -> Open -> trading-cordapp -> build.gradle -> OK -> Open as a Project

To debug PartyA node:
build nodes and run the party servers -> Run | Edit Configurations -> + -> Remote JVM debug -> Port 5005 -> OK -> enter breakpoints in Flow/Contract/etc.java -> run the debugger

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
<username> : TestAdmin-1 or ubuntu
<ip> : 20.108.166.202 (Azure) or 43.131.24.141 (Tencent)
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
$java -jar corda.jar --config-file=node.conf --base-directory=/opt/corda/PartyA/ --allow-hibernate-to-manage-app-schema &
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
3. start the node again, and add the run-migration-script sub-command: ``` run-migration-scripts --app-schemas --core-schemas```

