import React from 'react';

import {BrowserRouter as Router, Switch, Route} from "react-router-dom";
import './App.css';
import CreateTrade from './CreateTrade.js';

function App(props) {
    return (
        <Router>
            <div>
                <Switch>
                    <Route exact path="/" component={CreateTrade}/>
                </Switch>
            </div>
        </Router>
    );
}

export default App;