import React, {Component} from 'react';
// import Button from '@material-ui/core/Button';
import CssBaseline from '@material-ui/core/CssBaseline';
import TextField from '@material-ui/core/TextField';
import Grid from '@material-ui/core/Grid';
import {withStyles} from '@material-ui/core/styles';
import Container from '@material-ui/core/Container';
import axios from 'axios';
import corda_img from '../img/corda_img.png';
import {URL} from "../CONSTANTS";
import {FormControl, InputLabel, MenuItem, Select} from "@material-ui/core";
import {ToggleButton, ToggleButtonGroup} from "@material-ui/lab";
import AppBar from '@mui/material/AppBar';
import Box from '@mui/material/Box';
import Toolbar from '@mui/material/Toolbar';
import Typography from '@mui/material/Typography';
import Button from '@mui/material/Button';

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
            counterParty: "null",
            sellBuyValue: 0,
            sellBuyQuantity: 0,
            cashAmount: 1,
            stockAmount: 1,
            response: null,
            balance: 0,
            stockBalance: 0,
            alignment: "sell"
        }
    }

    async componentDidMount() {

        // Initialise to PartyA port
        if (localStorage.getItem('port') === null) {
            localStorage.setItem('port', '10056');
        }
        this.getAllNodes(); // call once when webpage mounts
        this.getPeers();
        this.getBalance();
        this.getStockQuantity();
    }

    issueStock = (e) => {
        const data = {
            amount: this.state.stockAmount
        }

        let PORT = localStorage.getItem('port');
        axios.post(URL + PORT + '/issueStock', data, {
            headers: {
                'Access-Control-Allow-Origin': '*',
                'Content-Type': 'application/json'
            }
        }).then(res => {
            const response = res.data.Response;
            if (response !== null) {
                console.log(response);
                if (response.includes("Success")) {
                    console.log("Stocks issued: " + res.data.Amount + ".");
                    window.location.reload();
                }
            }
        }).catch(e => {
            console.log(e);
        });

    }

    issueCash = (e) => {
        const data = {
            amount: this.state.cashAmount
        }

        let PORT = localStorage.getItem('port');
        axios.post(URL + PORT + '/issueCash', data, {
            headers: {
                'Access-Control-Allow-Origin': '*',
                'Content-Type': 'application/json'
            }
        }).then(res => {
            const response = res.data.Response;
            if (response !== null) {
                console.log(response);
                if (response.includes("Success")) {
                    console.log("Cash issued: " + res.data.Amount + " GBP.");
                    window.location.reload();
                }
            }
        }).catch(e => {
            console.log(e);
        });

    }

    getStockQuantity() {
        let PORT = localStorage.getItem('port');
        axios.get(URL + PORT + "/getStockList", {
            headers: {
                'Access-Control-Allow-Origin': '*',
                'Content-Type': 'application/json'
            }
        }).then(res => {
            console.log(res.data)
            let stockBalance = res.data;
            if (stockBalance.length !== 0) {
                this.setState({stockBalance: res.data[0]});
            }
        });
    }

    getBalance() {
        let PORT = localStorage.getItem('port');
        axios.get(URL + PORT + "/getCashBalance", {
            headers: {
                'Access-Control-Allow-Origin': '*',
                'Content-Type': 'application/json'
            }
        }).then(res => {
            this.setState({balance: res.data.Amount});
        });
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
        // this.getPeers();
        // this.getBalance();
        // this.getStockQuantity();
        // reloading the page triggers componentDidMount
        window.location.reload();
    }
    counterPartyChange = (e) => {
        this.setState({counterParty: e.target.value});
    }
    stockAmountChange = (e) => {
        this.setState({stockAmount: e.target.value});
    }
    cashAmountChange = (e) => {
        this.setState({cashAmount: e.target.value});
    }
    sellBuyValueChange = (e) => {
        this.setState({sellBuyValue: e.target.value});
    }
    sellBuyQuantityChange = (e) => {
        this.setState({sellBuyQuantity: e.target.value});
    }
    toggleHandleChange = (e, alignment) => {
        this.setState({alignment})
    }

    buttonHandler = (e) => {
        e.preventDefault();

        if (this.state.sellBuyValue === 0 && this.state.sellBuyQuantity === 0) {
            window.alert("Cannot create trade with 0 values.")
        } else {

            let data = {};
            if (this.state.alignment === "sell") {
                data = {
                    counterParty: this.state.counterParty,
                    sellValue: this.state.sellBuyValue,
                    sellQuantity: this.state.sellBuyQuantity,
                    buyValue: 0.0,
                    buyQuantity: 0,
                }
            } else if (this.state.alignment === "buy") {
                data = {
                    counterParty: this.state.counterParty,
                    sellValue: 0.0,
                    sellQuantity: 0,
                    buyValue: this.state.sellBuyValue,
                    buyQuantity: this.state.sellBuyQuantity,
                }
            }
            console.log(data);

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
        }

    };

    redirectToTrades = () => {
        const {history} = this.props;
        if (history) history.push('/trades');
    }


    render() {
        const {classes} = this.props;
        return (
            <div>
                <Container component="main" maxWidth="md">
                    <Box sx={{flexGrow: 1}}>
                        <AppBar position="static">
                            <Toolbar style={{marginRight: "20px"}}>
                                <img src={corda_img} style={{width: "100px"}} alt="corda logo"/>
                                <Typography variant="h7" component="div" sx={{flexGrow: 1}}>
                                    <h2>{localStorage.getItem('currentNode')}</h2>
                                </Typography>
                                <h3>
                                    Balance: <br/> {this.state.balance} GBP <br/>
                                    {this.state.stockBalance} Stocks
                                </h3>
                            </Toolbar>
                        </AppBar>
                    </Box>
                </Container>

                <Container component="main" maxWidth="sm">

                    <CssBaseline/>
                    <div className={classes.paper}>

                        <Typography component="h1" variant="h2">
                            Trading CordApp
                        </Typography>

                        <br/><br/>


                        <FormControl required fullWidth>
                            <InputLabel id="demo-simple-select-label">Initiating Party</InputLabel>
                            <Select
                                value={localStorage.getItem('currentNode')}
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
                        <br/>

                        <form className={classes.form} id="issueCashForm" noValidate>
                            <TextField
                                name="cashAmount"
                                variant="outlined"
                                fullWidth
                                id="cashAmount"
                                label="Amount (GBP)"
                                placeholder=""
                                onChange={this.cashAmountChange}
                                error={this.state.cashAmount === ""}
                                helperText={this.state.cashAmount === "" ? 'Empty field!' : ' '}
                                InputProps={{
                                    endAdornment: <Button
                                        fullWidth
                                        type="submit"
                                        variant="contained"
                                        color="primary"
                                        onClick={this.issueCash}>
                                        Issue Money
                                    </Button>
                                }}
                            />
                        </form>

                        <form className={classes.form} id="issueStockForm" noValidate>
                            <TextField
                                name="stockAmount"
                                variant="outlined"
                                fullWidth
                                id="stockAmount"
                                label="Stock Amount"
                                placeholder=""
                                onChange={this.stockAmountChange}
                                error={this.state.stockAmount === ""}
                                helperText={this.state.stockAmount === "" ? 'Empty field!' : ' '}
                                InputProps={{
                                    endAdornment: <Button
                                        fullWidth
                                        type="submit"
                                        variant="contained"
                                        color="primary"
                                        onClick={this.issueStock}>
                                        Issue Stocks
                                    </Button>
                                }}
                            />
                        </form>
                        <br/><br/>


                        <Typography component="h1" variant="h3">
                            Create a Trade
                        </Typography>

                        <form className={classes.form} id="createTradeForm" noValidate>

                            <ToggleButtonGroup
                                color="primary"
                                value={this.state.alignment}
                                exclusive
                                onChange={this.toggleHandleChange}>
                                <ToggleButton value="sell">Sell</ToggleButton>
                                <ToggleButton value="buy">Buy</ToggleButton>
                            </ToggleButtonGroup>
                            <br/><br/>

                            <Grid container spacing={2}>

                                <Grid item xs={12} sm={6}>
                                    <TextField
                                        autoComplete="fname"
                                        name="sellBuyValue"
                                        variant="outlined"
                                        required
                                        fullWidth
                                        id="sellBuyValue"
                                        label="Stock Value (GBP)"
                                        placeholder=""
                                        onChange={this.sellBuyValueChange}
                                        error={this.state.sellBuyValue === ""}
                                        helperText={this.state.sellBuyValue === "" ? 'Empty field!' : ' '}
                                    />
                                </Grid>
                                <Grid item xs={12} sm={6}>

                                    <TextField
                                        variant="outlined"
                                        required
                                        fullWidth
                                        id="sellBuyQuantity"
                                        label="Stock Quantity"
                                        name="sellBuyQuantity"
                                        autoComplete="sellBuyQuantity"
                                        placeholder=""
                                        onChange={this.sellBuyQuantityChange}
                                        error={this.sellBuyQuantity === ""}
                                        helperText={this.sellBuyQuantity === "" ? 'Empty field!' : ' '}
                                    />
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
                            <br/><br/>
                            <Button
                                fullWidth
                                type="submit"
                                variant="contained"
                                color="primary"
                                onClick={this.redirectToTrades}>
                                Trades
                            </Button>
                            <br/><br/><br/>
                        </form>
                    </div>
                </Container>
            </div>
        );
    }
}

export default withStyles(useStyles)(CreateTrade);