import React, {Component} from 'react';
import axios from "axios";
import {URL} from '../CONSTANTS.js';

class Me extends Component {
    constructor(props) {
        super(props);
        this.state = {
            node: "N/A",
        }
    }

    async componentDidMount() {
        let PORT = localStorage.getItem('port');
        axios.get(URL + PORT + "/node", {
            headers: {
                'Access-Control-Allow-Origin': '*',
                'Content-Type': 'application/json'
            }
        })
            .then(res => {
                console.log(res.data);
                this.setState({node: res.data.name})
            });
    }

    render() {
        return (
            <div>
                <h1>{this.state.node}</h1>
            </div>
        );
    }
}

export default Me;