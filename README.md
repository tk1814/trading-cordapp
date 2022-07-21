# Corda Trading App
Open the project in Intellij:
- File -> Open -> trading-cordapp -> build.gradle -> OK -> Open as a Project

To debug PartyA node:
build nodes and run the party servers -> Run | Edit Configurations -> + -> Remote JVM debug -> Port 5005 -> OK -> enter breakpoints in Flow/Contract/etc.java -> run the debugger

Run in terminal 1:
```
chmod +x gradlew (run only once)
./gradlew deployNodes
./build/nodes/runnodes
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
    
To install UI packages:
    - npm install @material-ui/lab
    - npm install @mui/material @emotion/react @emotion/styled
    - npm install @mui/icons-material
    - npm install @mui/lab

To start the react server:
    - cd clients/src/main/webapp
    - npm install 
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

