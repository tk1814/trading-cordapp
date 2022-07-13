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