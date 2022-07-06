import React from 'react';

import {
  BrowserRouter as Router,
  Switch,
  Route
} from "react-router-dom";

import './App.css';

import CreateTrade from '../CreateTrade/CreateTrade.js';
import Me from './Me';

function App(props) {

  return (
    <Router>
      <div>
        <Switch>
          <Route exact path="/" component={CreateTrade}/>
          <Route path="/create" component={CreateTrade}/>
          <Route path="/me" component={Me}/>
        </Switch>
      </div>
    </Router>
  );
}

export default App;