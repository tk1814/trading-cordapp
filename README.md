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

To start the react server:
    - cd clients/src/main/webapp
    - npm install 
    - npm start
```

If you change a .java file, you need to kill all the processes/servers (pkill -9 java) and rerun the commands shown above

PartyA connects with webserver on port 10056.
PartyB connects with webserver on port 10057.

[comment]: <> (# Navigate to:)

[comment]: <> (1. PartyA: `http://localhost:10009`)

[comment]: <> (2. PartyB: `http://localhost:10012`)

[comment]: <> (# Corda Secret Santa)

[comment]: <> (This is an implementation of Secret Santa using Corda as a tool to store multiple game states.)

[comment]: <> (It has a material-ui frontend that lets users create and self-service their own secret santa games. The frontend is implemented in ReactJS and the backend is implemented with a Spring Boot server and some corda flows.)

[comment]: <> (You can create a game using the web frontend &#40;or just calling the api directly with Postman&#41;, and once the game is stored, players can look up their assignments using their game id, and the app also supports an optional sendgrid integration so that you can have emails sent to the players as well.)

[comment]: <> (> One tip if you're using intellij is to open the project from the intellij dialog, don't import the project directly.)

[comment]: <> (## Usage)

[comment]: <> (There's essentially five processes you'll need to be aware of.)

[comment]: <> (- Three Corda nodes, a notary, santa, and an elf)

[comment]: <> (- The backend webserver that runs the REST endpoints for the corda nodes)

[comment]: <> (- The frontend webserver, a react app that sends requests to the backend.)


[comment]: <> (#### Pre-Requisites)

[comment]: <> (If you've never built a cordapp before you may need to configure gradle and java in order for this code example to run. See [our setup guide]&#40;https://docs.corda.net/getting-set-up.html&#41;.)


[comment]: <> (### Running these services)

[comment]: <> (#### The three Corda nodes)

[comment]: <> (To run the corda nodes you just need to run the `deployNodes` gradle task and the nodes will be available for you to run directly.)

[comment]: <> (```)

[comment]: <> (./gradlew deployNodes)

[comment]: <> (./build/nodes/runnodes)

[comment]: <> (```)

[comment]: <> (#### The backend webserver)

[comment]: <> (Run the `runTradingServer` Gradle task &#40;in a different terminal than the above&#41;. By default, it connects to the node with RPC address `localhost:10006` with)

[comment]: <> (the username `user1` and the password `test`, and serves the webserver on port `localhost:10056`.)

[comment]: <> (```)

[comment]: <> (./gradlew runTradingServer)

[comment]: <> (```)

[comment]: <> (The frontend will be visible on [localhost:10056]&#40;http://localhost:10056&#41;)

[comment]: <> (##### Background Information)

[comment]: <> (`clients/src/main/java/com/trading/webserver/` defines a simple Spring webserver that connects to a node via RPC and allows you to interact with the node over HTTP.)

[comment]: <> (The API endpoints are defined in `clients/src/main/java/com/trading/webserver/Controller.java`)


[comment]: <> (#### The frontend webserver)

[comment]: <> (The react server can be started &#40;in a different terminal&#41; by going to `clients/src/main/webapp`, running `npm install` and then `npm start`.)

[comment]: <> (```)

[comment]: <> (cd clients/src/main/webapp)

[comment]: <> (npm install)

[comment]: <> (npm start)

[comment]: <> (```)

[comment]: <> (The frontend will be visible on [localhost:8888]&#40;http://localhost:8888&#41;)

[comment]: <> (#### Configuring Email with SendGrid)

[comment]: <> (If you'd like to start sending email you'll need to make an account on [sendgrid.com]&#40;http://sendgrid.com&#41; and configure a verified sender identity.)

[comment]: <> (Once you've done that, create an API key and place it into `Controller.java`&#40;the webserver for the corda nodes&#41;. After which point you can set the `sendEmail` param to `true` in your requests. In order to configure the frontend to send emails, just open `CONSTANTS.js` and set the `SEND_EMAIL` param to `true` instead of `false`.)


[comment]: <> (### Testing Utilities)


[comment]: <> (#### Using Postman for backend testing)

[comment]: <> (I've included some simple postman tests to run against the santa server that will be helpful to you if you plan on using this. You'll find them in the `postman` folder.)


[comment]: <> (#### Running tests inside IntelliJ)

[comment]: <> (There are unit tests for the corda state, contract, and tests for both flows used here. You'll find them inside of the various test folders.)

