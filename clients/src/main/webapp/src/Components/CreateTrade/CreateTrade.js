import React, {Component} from 'react';
import Button from '@material-ui/core/Button';
import CssBaseline from '@material-ui/core/CssBaseline';
import TextField from '@material-ui/core/TextField';
import Grid from '@material-ui/core/Grid';
import Alert from '@material-ui/lab/Alert';
import Typography from '@material-ui/core/Typography';
import {withStyles} from '@material-ui/core/styles';
import Container from '@material-ui/core/Container';
import axios from 'axios';
import corda_img from '../img/corda_img.png';
import {URL} from "../CONSTANTS";
import {FormControl, InputLabel, MenuItem, Select} from "@material-ui/core";

const useStyles = (theme) => ({
    paper: {
        marginTop: theme.spacing(8),
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
    },
    avatar: {
        margin: theme.spacing(1),
        backgroundColor: theme.palette.secondary.main,
    },
    form: {
        width: '100%', // Fix IE 11 issue.
        marginTop: theme.spacing(3),
    },
    submit: {
        margin: theme.spacing(3, 0, 2),
    },
});

class CreateTrade extends Component {
    constructor(props) {
        super(props);
        this.state = {
            nodes: [],
            peers: [],
            counterParty: null,
            sellValue: 1,
            sellCurrency: "GBP",
            buyValue: 1,
            buyCurrency: "EUR",
            response: null,
        }
    }

    async componentDidMount() {

        // Initialise to PartyA port
        if (localStorage.getItem('port') === null) {
            localStorage.setItem('port', '10056');
        }
        this.getAllNodes(); // call once when webpage mounts
        this.getPeers();
    }

    getAllNodes() {
        let PORT = localStorage.getItem('port');
        axios.get(URL + PORT + "/nodes", {
            headers: {
                'Access-Control-Allow-Origin': '*',
                'Content-Type': 'application/json'
            }
        }).then(res => {
            let nodes = [];
            res.data.nodes.forEach(function (item, index) {
                nodes[index] = item.x500Principal.name;
            });
            this.setState({nodes});
        });
    }

    getPeers() {
        let PORT = localStorage.getItem('port');
        axios.get(URL + PORT + "/peers", {
            headers: {
                'Access-Control-Allow-Origin': '*',
                'Content-Type': 'application/json'
            }
        }).then(res => {
            let peers = [];
            res.data.peers.forEach(function (item, index) {
                peers[index] = item.x500Principal.name;
            });
            this.setState({peers});
        });
    }

    initiatingPartyChange = (e) => {
        let node = e.target.value;
        localStorage.setItem('currentNode', node);

        if (node.includes("PartyA")) {
            localStorage.setItem('port', '10056');
        } else if (node.includes("PartyB")) {
            localStorage.setItem('port', '10057');
        }
        this.getPeers();
    }
    counterPartyChange = (e) => {
        this.setState({counterParty: e.target.value});
    }
    buyValueChange = (e) => {
        this.setState({buyValue: e.target.value});
    }
    sellValueChange = (e) => {
        this.setState({sellValue: e.target.value});
    }
    buyCurrencyChange = (e) => {
        this.setState({buyCurrency: e.target.value});
    }
    sellCurrencyChange = (e) => {
        this.setState({sellCurrency: e.target.value});
    }

    buttonHandler = (e) => {
        e.preventDefault();

        console.log([this.state.counterParty, this.state.sellValue, this.state.sellCurrency, this.state.buyValue, this.state.buyCurrency]);

        const data = {
            counterParty: this.state.counterParty,
            sellValue: this.state.sellValue,
            sellCurrency: this.state.sellCurrency,
            buyValue: this.state.buyValue,
            buyCurrency: this.state.buyCurrency,
        }

        let PORT = localStorage.getItem('port');
        axios.post(URL + PORT + '/createTrade', data, {
            headers: {
                'Access-Control-Allow-Origin': '*',
                'Content-Type': 'application/json'
            }
        }).then(res => {

            const response = res.data.Response;
            if (response !== null) {
                console.log(response);
                this.setState({response});
                if (response.includes("committed to ledger")) {
                    this.redirectToTrades();
                }
            }
        }).catch(e => {
            console.log(e);
        });

    };

    redirectToTrades = () => {
        const {history} = this.props;
        if (history) history.push('/trades');
    }

    render() {
        const {classes} = this.props;
        return (
            <Container component="main" maxWidth="sm">
                <CssBaseline/>
                <div className={classes.paper}>

                    {/*<Alert onClose={() => {}}> {this.state.response}</Alert>*/}

                    <h2>{localStorage.getItem('currentNode')}</h2>

                    <img src={corda_img} alt="corda logo"/>

                    <Typography component="h1" variant="h2">
                        Trading CordApp
                    </Typography>

                    <FormControl required fullWidth>
                        <InputLabel id="demo-simple-select-label">Initiating Party</InputLabel>
                        <Select
                            defaultValue={''}
                            labelId="demo-simple-select-label"
                            id="demo-simple-select"
                            label="Party"
                            onChange={this.initiatingPartyChange}>
                            {this.state.nodes.map((node, key) => (
                                <MenuItem
                                    key={key}
                                    value={node}>{node}
                                </MenuItem>))}
                        </Select>
                    </FormControl>

                    <form className={classes.form} id="createTradeForm" noValidate>

                        <Grid container spacing={2}>

                            <Grid item xs={12} sm={12}>

                                <FormControl required fullWidth>
                                    <InputLabel id="demo-simple-select-label">Counter Party</InputLabel>
                                    <Select
                                        defaultValue={''}
                                        name="counterParty"
                                        id="counterParty"
                                        label="Counter Party"
                                        onChange={this.counterPartyChange}>
                                        {this.state.peers.map((peer, key) => (
                                            <MenuItem
                                                key={key}
                                                value={peer}>{peer}
                                            </MenuItem>))}
                                    </Select>
                                </FormControl>
                            </Grid>

                            <Grid item xs={12} sm={6}>
                                <TextField
                                    autoComplete="fname"
                                    name="sellValue"
                                    variant="outlined"
                                    required
                                    fullWidth
                                    id="sellValue"
                                    label="Sell Value"
                                    placeholder=""
                                    onChange={this.sellValueChange}
                                    error={this.state.sellValue === ""}
                                    helperText={this.state.sellValue === "" ? 'Empty field!' : ' '}
                                />
                            </Grid>
                            <Grid item xs={12} sm={6}>
                                <TextField
                                    variant="outlined"
                                    required
                                    fullWidth
                                    id="sellCurrency"
                                    label="Sell Currency"
                                    name="sellCurrency"
                                    autoComplete="sellCurrency"
                                    // type="email"
                                    placeholder="GBP"
                                    onChange={this.sellCurrencyChange}
                                    error={this.sellCurrency === ""}
                                    helperText={this.sellCurrency === "" ? 'Empty field!' : ' '}
                                />
                            </Grid>

                            <Grid item xs={12} sm={6}>
                                <TextField
                                    autoComplete="fname"
                                    name="buyValue"
                                    variant="outlined"
                                    required
                                    fullWidth
                                    id="buyValue"
                                    label="Buy Value"
                                    placeholder=""
                                    onChange={this.buyValueChange}
                                    error={this.state.buyValue === ""}
                                    helperText={this.state.buyValue === "" ? 'Empty field!' : ' '}
                                />
                            </Grid>
                            <Grid item xs={12} sm={6}>
                                <TextField
                                    variant="outlined"
                                    required
                                    fullWidth
                                    id="buyCurrency"
                                    label="Buy Currency"
                                    name="buyCurrency"
                                    // autoComplete="email"
                                    type="text"
                                    placeholder="EUR"
                                    onChange={this.buyCurrencyChange}
                                    error={this.state.buyCurrency === ""}
                                    helperText={this.state.buyCurrency === "" ? 'Empty field!' : ' '}
                                />
                            </Grid>

                            <Grid item xs={12}>
                            </Grid>
                        </Grid>

                        <Button
                            type="submit"
                            fullWidth
                            variant="contained"
                            color="primary"
                            onClick={this.buttonHandler}>
                            Create Trade
                        </Button>
                    </form>
                </div>
            </Container>
        );
    }
}

export default withStyles(useStyles)(CreateTrade);