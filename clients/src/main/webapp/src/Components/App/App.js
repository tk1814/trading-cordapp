import React from 'react';

import {
  BrowserRouter as Router,
  Switch,
  Route
} from "react-router-dom";

import './App.css';

import CreateTrade from '../CreateTrade/CreateTrade.js';
import CheckSantaGame from '../CheckSantaGame/CheckSantaGame';
import SantaGameCreated from '../SantaGameCreated/SantaGameCreated';
import SantaCheckSent from '../SantaCheckSent/SantaCheckSent';

function App(props) {

  return (
    <Router>
      <div>
        <Switch>
          <Route exact path="/" component={CreateTrade}/>
          <Route path="/created" component={SantaGameCreated}/>
          <Route path="/create" component={CreateTrade}/>
          <Route path="/checked" component={SantaCheckSent}/>
          <Route path="/check" component={CheckSantaGame}/>          
        </Switch>
      </div>
    </Router>
  );
}

export default App;