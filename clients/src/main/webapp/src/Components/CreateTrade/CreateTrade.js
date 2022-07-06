import React, {Component} from 'react';
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
import Drawer from '@mui/material/Drawer';
import List from '@mui/material/List';
import Divider from '@mui/material/Divider';
import ListItem from '@mui/material/ListItem';
import ListItemText from '@mui/material/ListItemText';
import TableContainer from "@mui/material/TableContainer";
import Paper from "@mui/material/Paper";
import Table from "@mui/material/Table";
import TableHead from "@mui/material/TableHead";
import TableRow from "@mui/material/TableRow";
import TableCell from "@mui/material/TableCell";
import TableBody from "@mui/material/TableBody";
import moment from "moment";

const drawerWidth = 300;
const headers = {'Access-Control-Allow-Origin': '*', 'Content-Type': 'application/json'};

const useStyles = (theme) => ({
    paper: {
        marginTop: theme.spacing(7),
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
            orderType: "pendingOrder",
            value: "1",
            trades: [],
            nodes: [],
            peers: [],
            counterParty: "null",
            stockPrice: 0,
            stockQuantity: 0,
            stockAmount: 1,
            moneyAmount: 1,
            stockName: null,
            response: null,
            balance: 0,
            stockBalanceList: [],
            stockNames: [],
            stockToTrade: null,
            alignment: "sell",
            bidPrice: "--",
            askPrice: "--",
            intervalId: null,
            expirationDate: moment(new Date()).add(10, 'm').toDate().toISOString().slice(0, 16),
            stockCodes: ["AAPL", "AMZN", "TSLA", "NFLX", "META", "GOOG", "TWTR"]
        }
    }


    stockAmountChange = (e) => {
        this.setState({stockAmount: e.target.value});
    }
    stockNameChange = (e) => {
        this.setState({stockName: e.target.value});
    }
    moneyAmountChange = (e) => {
        this.setState({moneyAmount: e.target.value});
    }
    stockPriceChange = (e) => {
        this.setState({stockPrice: e.target.value});
    }
    expirationDateChange = (e) => {
        this.setState({expirationDate: e.target.value});
    }
    stockQuantityChange = (e) => {
        this.setState({stockQuantity: e.target.value});
    }
    toggleHandleChange = (e, alignment) => {
        this.setState({alignment})
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
        this.getCounterParty();
        this.getTrades();
    }

    issueStock = (e) => {
        if (this.state.stockNames.includes(this.state.stockName))
            window.alert("Cannot issue stock with an existing name.")
        else {
            const data = {
                amount: parseFloat(this.state.stockAmount).toFixed(0),
                name: this.state.stockName
            }

            let PORT = localStorage.getItem('port');
            axios.post(URL + PORT + '/issueStock', data, {
                headers: headers
            }).then(res => {
                const response = res.data.Response;
                if (response !== null) {
                    console.log(response);
                    if (response.includes("Success")) {
                        console.log(res.data.Name + "stocks issued: " + res.data.Amount + ".");
                        window.location.reload();
                    }
                }
            }).catch(e => {
                console.log(e);
            });
        }
    }

    issueMoney = (e) => {
        const data = {amount: parseFloat(this.state.moneyAmount).toFixed(2)}
        let PORT = localStorage.getItem('port');
        axios.post(URL + PORT + '/issueMoney', data, {
            headers: headers
        }).then(res => {
            const response = res.data.Response;
            if (response !== null) {
                console.log(response);
                if (response.includes("Success")) {
                    console.log("Cash issued: " + res.data.Amount);
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
            headers: headers
        }).then(res => {
            if (res.data.Response === "Success" && res.data.StockList !== "[]") {
                let stocks = [];
                let stockNames = [];

                let stockList = res.data.StockList.split(",");
                stockList.forEach(function (item, index) {
                    stockNames[index] = item.substring(item.indexOf('=') + 1).replace("]", "").replace("[", "");
                    stocks[index] = item.replace("=", " ").replace("]", "").replace("[", "");
                });
                this.setState({stockBalanceList: stocks});
                this.setState({stockNames});
            }
        });
    }

    getBalance() {
        let PORT = localStorage.getItem('port');
        axios.get(URL + PORT + "/getMoneyBalance", {
            headers: headers
        }).then(res => {
            this.setState({balance: res.data.Amount});
        });
    }

    getAllNodes() {
        let PORT = localStorage.getItem('port');
        axios.get(URL + PORT + "/nodes", {
            headers: headers
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
            headers: headers
        }).then(res => {
            let peers = [];
            res.data.peers.forEach(function (item, index) {
                peers[index] = item.x500Principal.name;
            });
            this.setState({peers});
        });
    }

    getCounterParty() {
        let PORT = localStorage.getItem('port');
        axios.get(URL + PORT + "/node", {
            headers: headers
        }).then(res => {
            let counterParty = res.data.name;
            this.setState({counterParty})
        }).catch(e => {
            console.log(e);
        });
    }

    getTrades() {
        let PORT = localStorage.getItem('port');
        axios.get(URL + PORT + "/trades", {
            headers: headers
        }).then(res => {
            let trades = res.data;
            trades.forEach(function (item, index) {
                trades[index] = item.split("|");
            });
            let invertedTrades = trades.reverse();
            this.setState({trades: invertedTrades});
        }).catch(e => {
            console.log(e);
        });
    }

    getPartyfromPort() {
        if (localStorage.getItem('port') === '10056') {
            return 'PartyA'
        } else if (localStorage.getItem('port') === '10057') {
            return 'PartyB'
        }
    }

    initiatingPartyChange = (e) => {
        let node = e.target.value;
        localStorage.setItem('currentNode', node);

        if (node.includes("PartyA")) {
            localStorage.setItem('port', '10056');
        } else if (node.includes("PartyB")) {
            localStorage.setItem('port', '10057');
        }
        // reloading the page triggers componentDidMount
        window.location.reload();
    }

    createPendingOrder = (e) => {
        e.preventDefault();

        let current = new Date().toISOString().slice(0, 16)
        let selected = this.state.expirationDate;
        let currentDateTime = new Date(current);
        let selectedDateTime = new Date(selected);
        let currentDate = current.slice(0, 10)

        // don't allow previous dates
        // if today's date is chosen: don't allow previous times
        if (this.state.stockPrice === 0 && this.state.stockQuantity === 0) {
            window.alert("Cannot create trade with 0 values.")
        } else if (this.state.expirationDate === null) {
            window.alert("Null expiration date.")
        } else if (selectedDateTime.getDate() < currentDateTime.getDate()) {
            window.alert("Invalid expiration day.")
        } else if (selectedDateTime.getMonth() < currentDateTime.getMonth()) {
            window.alert("Invalid expiration month.")
        } else if (selectedDateTime.getFullYear() < currentDateTime.getFullYear()) {
            window.alert("Invalid expiration year.")
        } else if (currentDate === this.state.expirationDate.slice(0, 10) && selectedDateTime.getTime() < currentDateTime.getTime()) {
            window.alert("Invalid expiration time.")
        } else {
            let tradeType;
            if (this.state.alignment === "sell") {
                tradeType = "Sell";
            } else if (this.state.alignment === "buy") {
                tradeType = "Buy";
            }
            let data = {
                counterParty: this.state.counterParty,
                orderType: "Pending Order",
                tradeType: tradeType,
                stockName: this.state.stockToTrade,
                stockPrice: parseFloat(this.state.stockPrice).toFixed(2),
                stockQuantity: this.state.stockQuantity,
                expirationDate: this.state.expirationDate
            }
            console.log(data);

            let PORT = localStorage.getItem('port');
            axios.post(URL + PORT + '/createTrade', data, {
                headers: headers
            }).then(res => {
                const response = res.data.Response;
                if (response !== null) {
                    console.log(response);
                    this.setState({response});
                    if (response.includes("committed to ledger")) {
                        window.location.reload();
                    }
                }
            }).catch(e => {
                console.log(e);
            });
        }
    };

    orderTypeChange = (e) => {
        let orderType = e.target.value;
        this.setState({orderType});
        // clear previous gets
        if (this.state.intervalId !== null) {
            clearInterval(this.state.intervalId);
            this.setState({intervalId: null});
        }
        this.setState({bidPrice: "--"});
        this.setState({askPrice: "--"});

        // get real time stock prices every 10 seconds
        if (orderType === "marketOrder" && this.state.stockToTrade !== null) {
            this.getStockToTradePrice(this.state.stockToTrade);
            let intervalId = setInterval(() => {
                this.getStockToTradePrice(this.state.stockToTrade);
            }, 10000);
            this.setState({intervalId})
        }
    }
    stockToTradeChange = (e) => {
        let stockToTrade = e.target.value;
        this.setState({stockToTrade});
        // clear previous gets
        if (this.state.intervalId !== null) {
            clearInterval(this.state.intervalId);
            this.setState({intervalId: null});
        }
        this.setState({bidPrice: "--"});
        this.setState({askPrice: "--"});

        if (this.state.orderType === "marketOrder" && stockToTrade !== null) {
            this.getStockToTradePrice(stockToTrade);
            // get real time stock prices every 10 seconds
            let intervalId = setInterval(() => {
                this.getStockToTradePrice(stockToTrade);
            }, 10000);
            this.setState({intervalId})
        }
    }

    getStockToTradePrice = (stockToTrade) => {
        let options = {
            method: 'GET',
            url: 'https://yfapi.net/v6/finance/quote',
            params: {region: 'GB', lang: 'en', symbols: stockToTrade},
            headers: {
                'Content-Type': 'application/json',
                'x-api-key': 'oxVJNIkhxP2Z4hVB1YE9W91crYWCLvzX5fM6TElC'
            }
        };

        // TODO: uncomment to fetch real-time stock quote & update API key
        axios.request(options).then(response => {
            if (response !== null) {
                let bidPrice = response.data.quoteResponse.result[0].bid;
                let askPrice = response.data.quoteResponse.result[0].ask;
                console.log(bidPrice, askPrice);
                if (bidPrice !== 0 && askPrice !== 0) {
                    console.log(bidPrice, askPrice);
                    this.setState({bidPrice});
                    this.setState({askPrice});
                } else {
                    window.alert("Cannot get stock quote. Market is not open.");
                    if (this.state.intervalId !== null) {
                        clearInterval(this.state.intervalId);
                        this.setState({intervalId: null});
                    }
                }
            }
        }).catch(e => {
            console.log(e);
        });
    }

    createMarketOrder(e, marketOrderType) {
        e.preventDefault();
        console.log(marketOrderType)

        if (this.state.stockQuantity === 0) {
            window.alert("Cannot create trade with 0 values.")
        } else {
            let tradeType, stockPrice;
            if (marketOrderType === "sellByMarket") {
                tradeType = "Sell";
                stockPrice = this.state.askPrice
            } else if (marketOrderType === "buyByMarket") {
                tradeType = "Buy";
                stockPrice = this.state.bidPrice
            }
            let data = {
                counterParty: this.state.counterParty,
                orderType: "Market Order",
                tradeType: tradeType,
                stockName: this.state.stockToTrade,
                stockPrice: stockPrice,
                stockQuantity: this.state.stockQuantity,
                expirationDate: moment(new Date()).add(3, 'm').toDate().toISOString().slice(0, 16) // sets 3 mins expiration date
            }
            console.log(data);

            let PORT = localStorage.getItem('port');
            axios.post(URL + PORT + '/createTrade', data, {
                headers: headers
            }).then(res => {
                const response = res.data.Response;
                if (response !== null) {
                    console.log(response);
                    this.setState({response});
                    if (response.includes("committed to ledger")) {
                        window.location.reload();
                    }
                }
            }).catch(e => {
                console.log(e);
            });
        }
    }

    counterTradeButton = (index, initiatingParty) => {
        let partyTrades = this.state.trades;

        const data = {
            initiatingParty: initiatingParty,
            counterParty: this.state.counterParty,
            orderType: partyTrades[index][2],
            tradeType: partyTrades[index][3],
            stockQuantity: partyTrades[index][4],
            stockName: partyTrades[index][5],
            stockPrice: partyTrades[index][6],
            expirationDate: partyTrades[index][7],
            tradeStatus: "Accepted",
            tradeID: partyTrades[index][9],
        }
        console.log(data)

        if (data.tradeType === "Sell") {
            // find port of initiating party
            let PORT;
            if (data.initiatingParty.includes("PartyA")) {
                PORT = "10056";
            } else if (data.initiatingParty.includes("PartyB")) {
                PORT = "10057";
            }
            // initiating party calls to move stocks from initiating party to counterparty
            axios.post(URL + PORT + '/counterTrade', data, {
                headers: headers
            }).then(res => {
                console.log(res.data.Response);
                window.location.reload();
            })
        } else if (data.tradeType === "Buy") {

            // counterparty calls to move stocks from counterparty to initiating party
            let PORT = localStorage.getItem('port');
            axios.post(URL + PORT + '/counterTrade', data, {
                headers: headers
            }).then(res => {
                console.log(res.data.Response);
                window.location.reload();
            })
        }
    }


    render() {
        const {classes} = this.props;
        return (
            <div>
                <CssBaseline/>
                <div className={classes.paper}>
                    <Container component="main" maxWidth="sm">

                        <Box sx={{display: 'flex'}}>
                            <CssBaseline/>
                            <AppBar position="fixed" sx={{
                                width: `calc(100% - ${drawerWidth}px)`,
                                ml: `${drawerWidth}px`
                            }}> </AppBar>
                            <Drawer sx={{
                                width: drawerWidth, flexShrink: 0, '& .MuiDrawer-paper': {
                                    width: drawerWidth, boxSizing: 'border-box',
                                },
                            }} variant="permanent" anchor="left">
                                <Toolbar>
                                    <img src={corda_img} style={{width: "80px", marginLeft: "30%"}} alt="corda logo"/>
                                </Toolbar>
                                <Divider/>
                                <br/>

                                <FormControl required fullWidth style={{marginLeft: "20px"}}>
                                    <Grid item xs={10}>
                                        <InputLabel id="demo-simple-select-label">Initiating Party</InputLabel>
                                        <Select
                                            value={localStorage.getItem('currentNode')}
                                            defaultValue={''}
                                            labelId="demo-simple-select-label"
                                            id="demo-simple-select"
                                            label="Party"
                                            onChange={this.initiatingPartyChange} fullWidth>
                                            {this.state.nodes.map((node, key) => (
                                                <MenuItem key={key} value={node}>{node}</MenuItem>))}
                                        </Select>
                                    </Grid>
                                </FormControl>
                                <br/>
                                <Divider/>

                                {/* ------------------ ORDERS ------------------------*/}
                                <Grid container spacing={2}>
                                    <Grid item xs={10} style={{marginLeft: "20px"}}>
                                        <FormControl required fullWidth>
                                            <InputLabel id="demo-simple-select-label">Stock</InputLabel>
                                            <Select
                                                // value={}
                                                defaultValue={''}
                                                labelId="demo-simple-select-label"
                                                id="demo-simple-select"
                                                label="stockToTrade"
                                                onChange={this.stockToTradeChange}>
                                                {this.state.stockNames.map((stock, key) => (
                                                    <MenuItem key={key} value={stock}>{stock} </MenuItem>))}
                                            </Select>
                                        </FormControl>
                                    </Grid>

                                    <Grid item xs={10} style={{marginLeft: "20px"}}>
                                        <FormControl required fullWidth>
                                            <Select
                                                value={this.state.orderType}
                                                defaultValue={"pendingOrder"}
                                                labelId="demo-simple-select-label"
                                                id="demo-simple-select"
                                                label="orderType"
                                                onChange={this.orderTypeChange} fullWidth>
                                                <MenuItem value="pendingOrder">Pending Order</MenuItem>
                                                <MenuItem value="marketOrder">Market Execution</MenuItem>
                                            </Select>
                                        </FormControl>
                                    </Grid>
                                </Grid>


                                {/* --------- Pending Order -------------- */}
                                {(this.state.orderType === "pendingOrder") &&

                                <form className={classes.form} style={{marginLeft: "20px"}} id="createTradeForm" noValidate>

                                    <ToggleButtonGroup size="small" color="primary" value={this.state.alignment} exclusive onChange={this.toggleHandleChange}>
                                        <ToggleButton value="sell">Sell</ToggleButton>
                                        <ToggleButton value="buy">Buy </ToggleButton>
                                    </ToggleButtonGroup>
                                    <br/><br/>

                                    <Grid container>
                                        <Grid item xs={10}>
                                            <TextField
                                                size="small"
                                                variant="outlined"
                                                required
                                                fullWidth
                                                id="stockQuantity"
                                                label="Stock Volume"
                                                name="stockQuantity"
                                                autoComplete="stockQuantity"
                                                placeholder=""
                                                onChange={this.stockQuantityChange}
                                                error={this.stockQuantity === ""}
                                                helperText={this.stockQuantity === "" ? 'Empty field!' : ' '}/>
                                        </Grid>
                                        <Grid item xs={10}>
                                            <TextField
                                                size="small"
                                                autoComplete="fname"
                                                name="stockPrice"
                                                variant="outlined"
                                                required
                                                fullWidth
                                                id="stockPrice"
                                                label="Stock Price (GBP)"
                                                InputProps={{style: {marginTop: "-10px"}}}
                                                InputLabelProps={{style: {marginTop: "-10px"}}}
                                                placeholder=""
                                                onChange={this.stockPriceChange}
                                                error={this.state.stockPrice === ""}
                                                helperText={this.state.stockPrice === "" ? 'Empty field!' : ' '}/>
                                        </Grid>

                                        <Grid item xs={10}>
                                            <TextField
                                                fullWidth
                                                defaultValue={moment(new Date()).add(10, 'm').toDate().toISOString().slice(0, 16)}
                                                id="expirationDate"
                                                label="Expiration Date"
                                                inputProps={{
                                                    min: new Date().toISOString().slice(0, 16),
                                                    style: {}
                                                }}
                                                InputLabelProps={{shrink: true, style: {}}}
                                                type="datetime-local"
                                                onChange={this.expirationDateChange}/>
                                        </Grid>
                                        <br/><br/><br/>

                                        <Grid item xs={10}>
                                            <Button
                                                size="small"
                                                type="submit"
                                                fullWidth
                                                variant="contained"
                                                color="primary"
                                                onClick={this.createPendingOrder}>
                                                Place Order
                                            </Button>
                                        </Grid>
                                    </Grid>
                                </form>}


                                {/* --------- Market Order -------------- */}
                                {(this.state.orderType === "marketOrder") &&
                                <form className={classes.form} style={{marginLeft: "20px"}}
                                      id="createMarketForm" noValidate>

                                    <Grid container size="small">
                                        <Grid item xs={10}>
                                            <TextField
                                                size="small"
                                                variant="outlined"
                                                required
                                                fullWidth
                                                id="stockQuantity"
                                                label="Stock Volume"
                                                name="stockQuantity"
                                                autoComplete="stockQuantity"
                                                InputProps={{style: {}}}
                                                InputLabelProps={{style: {}}}
                                                placeholder=""
                                                onChange={this.stockQuantityChange}
                                                error={this.stockQuantity === ""}
                                                helperText={this.stockQuantity === "" ? 'Empty field!' : ' '}/>
                                        </Grid>
                                        <Grid item xs={10}>
                                            <p style={{
                                                marginLeft: "-20px", marginTop: "-10px",
                                                fontSize: 17, display: "flex",
                                                justifyContent: "center"
                                            }}>{this.state.askPrice} / {this.state.bidPrice} </p>
                                        </Grid>
                                        <Grid container spacing={2}>
                                            <Grid item xs={10}>
                                                <Button
                                                    size="small"
                                                    type="submit"
                                                    fullWidth
                                                    variant="contained"
                                                    color="primary"
                                                    onClick={(e) => this.createMarketOrder(e, "sellByMarket")}>
                                                    Sell by Market
                                                </Button>
                                            </Grid>
                                            <Grid item xs={10}>
                                                <Button
                                                    size="small"
                                                    type="submit"
                                                    fullWidth
                                                    variant="contained"
                                                    color="primary"
                                                    onClick={(e) => this.createMarketOrder(e, "buyByMarket")}>
                                                    Buy By Market
                                                </Button>
                                            </Grid>
                                        </Grid>
                                    </Grid>
                                </form>}

                                <br/>
                                <Divider/>

                                <form className={classes.form} style={{marginLeft: "20px"}} id="issueMoneyForm" noValidate>
                                    <Grid item xs={10}>
                                        <TextField
                                            required
                                            size="small"
                                            name="moneyAmount"
                                            variant="outlined"
                                            fullWidth
                                            id="moneyAmount"
                                            label="Amount (GBP)"
                                            InputProps={{style: {}}}
                                            InputLabelProps={{style: {}}}
                                            placeholder=""
                                            onChange={this.moneyAmountChange}
                                            error={this.state.moneyAmount === ""}
                                            helperText={this.state.moneyAmount === "" ? 'Empty field!' : ' '}/>
                                    </Grid>
                                    <Grid item xs={10}>
                                        <Button
                                            style={{marginTop: "-10px"}}
                                            size="small"
                                            fullWidth
                                            type="submit"
                                            variant="contained"
                                            color="primary"
                                            onClick={this.issueMoney}>
                                            Issue Money
                                        </Button>
                                    </Grid>
                                </form>

                                <br/>
                                <Divider/>

                                <form className={classes.form} style={{
                                    marginLeft: "20px",
                                    marginTop: "0px"
                                }} id="issueStockForm" noValidate>

                                    <Grid container spacing={2}>

                                        <Grid item xs={10}>
                                            <FormControl required fullWidth>
                                                <InputLabel id="demo-simple-select-label">Stock</InputLabel>
                                                <Select
                                                    // value={}
                                                    defaultValue={''}
                                                    labelId="demo-simple-select-label"
                                                    id="demo-simple-select"
                                                    label="Stock Name"
                                                    onChange={this.stockNameChange}>
                                                    {this.state.stockCodes.map((stock, key) => (
                                                        <MenuItem key={key} value={stock}>{stock} </MenuItem>))}
                                                </Select>
                                            </FormControl>
                                        </Grid>
                                        <Grid item xs={10}>
                                            <TextField
                                                size="small"
                                                required
                                                name="stockAmount"
                                                variant="outlined"
                                                fullWidth
                                                id="stockAmount"
                                                label="Stock Amount"
                                                onChange={this.stockAmountChange}
                                                error={this.state.stockAmount === ""}
                                                helperText={this.state.stockAmount === "" ? 'Empty field!' : ' '}
                                            />
                                        </Grid>
                                        <Grid item xs={10}>
                                            <Button
                                                style={{marginTop: "-40px"}}
                                                size="small"
                                                fullWidth
                                                type="submit"
                                                variant="contained"
                                                color="primary"
                                                onClick={this.issueStock}>
                                                Issue Stocks
                                            </Button>
                                        </Grid>
                                    </Grid>
                                </form>
                                <br/>
                            </Drawer>
                        </Box>
                    </Container>


                    {/* ------------- Stocks ------------- */}
                    <Container component="main" maxWidth="sm">
                        <Box sx={{display: 'flex'}}>
                            <CssBaseline/>
                            <AppBar position="fixed"
                                    sx={{width: `calc(100% - ${drawerWidth}px)`, ml: `${drawerWidth}px`}}> </AppBar>
                            <Drawer sx={{
                                width: drawerWidth, flexShrink: 0, '& .MuiDrawer-paper': {
                                    width: drawerWidth, boxSizing: 'border-box',
                                },
                            }} variant="permanent" anchor="right">
                                <Toolbar/>
                                <br/>
                                <Divider/>
                                <p style={{marginLeft: "20px", fontSize: 17, fontWeight: 'bold'}}>Balance:</p>
                                <p style={{marginLeft: "20px", fontSize: 17, marginTop: "-10px"}}>
                                    {this.state.balance}</p>
                                <Divider/>
                                {(this.state.stockBalanceList.length !== 0) ?
                                    <List style={{marginLeft: "20px", fontSize: 14.5}}>
                                        <h3 style={{marginBottom: "3px"}}>Stocks:</h3>
                                        {this.state.stockBalanceList.map((stock, index) => (
                                            <ListItem key={index} disablePadding><ListItemText primary={stock}/></ListItem>
                                        ))}
                                    </List> :
                                    <ListItem key={0}><h3 style={{marginLeft: "4px", fontSize: 17}}>No issued
                                        stocks</h3></ListItem>}
                                <Divider/>
                            </Drawer>
                        </Box>
                    </Container>


                    {/* ------------- TRADE HISTORY TABLE ------------- */}
                    <Container component="main" maxWidth="lg" style={{width: `calc(100% - 2*${drawerWidth}px)`}}>
                        <CssBaseline/>
                        <div className={classes.paper} style={{marginTop: "-22px"}}>
                            <Typography component="h1" variant="h5">Trade History</Typography>
                            <br/>
                            <TableContainer className={classes.table} component={Paper}
                                            style={{maxHeight: "17rem", overflow: "auto"}}>
                                <Table sx={{minWidth: 650}} size="small" aria-label="simple table">
                                    <TableHead>
                                        <TableRow>
                                            <TableCell>Initiating Party</TableCell>
                                            <TableCell>Counter Party</TableCell>
                                            <TableCell align="left">Order Type</TableCell>
                                            <TableCell align="left">Type</TableCell>
                                            <TableCell align="left">Volume</TableCell>
                                            <TableCell align="left">Stock</TableCell>
                                            <TableCell align="left">Price</TableCell>
                                            <TableCell align="left">Trade status</TableCell>
                                            <TableCell align="left">Trade ID</TableCell>
                                        </TableRow>
                                    </TableHead>
                                    <TableBody>
                                        {this.state.trades.map((row, index) => (
                                            <TableRow key={index}
                                                      sx={{'&:last-child td, &:last-child th': {border: 0}}}>
                                                {row.map((item, idx) =>
                                                    // Display only accepted trades
                                                    // don't show expiration date
                                                    (row[8] === "Accepted") && (!item.includes(":")) &&
                                                    (<TableCell key={idx} component="th" scope="row">{item}</TableCell>)
                                                )}
                                            </TableRow>
                                        ))}
                                    </TableBody>
                                </Table>
                            </TableContainer>
                            <br/><br/>
                            {/*  ----------- MARKET ORDERS TABLE ------------*/}
                            <Typography component="h1" variant="h5">Market Orders</Typography> <br/>
                            <TableContainer className={classes.table} component={Paper}
                                            style={{maxHeight: "14rem", overflow: "auto"}}>

                                <Table sx={{minWidth: 650}} size="small" aria-label="simple table">
                                    <TableHead>
                                        <TableRow>
                                            <TableCell>Initiating Party</TableCell>
                                            <TableCell align="left">Order Type</TableCell>
                                            <TableCell align="left">Type</TableCell>
                                            <TableCell align="left">Volume</TableCell>
                                            <TableCell align="left">Stock</TableCell>
                                            <TableCell align="left">Price</TableCell>
                                            <TableCell align="left">Expiration Date</TableCell>
                                            <TableCell align="left">Trade status</TableCell>
                                            <TableCell align="left">Trade ID</TableCell>
                                            <TableCell align="left"></TableCell>
                                        </TableRow>
                                    </TableHead>

                                    <TableBody>
                                        {this.state.trades.map((row, index) => (
                                            <TableRow key={index}
                                                      sx={{'&:last-child td, &:last-child th': {border: 0}}}>
                                                {row.map((item, idx) =>
                                                    // Display only pending trades
                                                    (row[8] === "Pending") && (item !== "null") && (row[2] === "Market Order") &&
                                                    (<TableCell key={idx} component="th" scope="row">{item}</TableCell>)
                                                )}
                                                {/* Initiating Party cannot counter trade their trade */}
                                                {(row[8] === "Pending") && (!row[0].includes(this.getPartyfromPort())) && (row[2] === "Market Order") &&
                                                <TableCell component="th" scope="row">
                                                    <Button
                                                        size='small'
                                                        style={{marginBottom: 5}}
                                                        type="submit"
                                                        fullWidth
                                                        variant="contained"
                                                        color="primary"
                                                        onClick={() => this.counterTradeButton(index, row[0])}>Accept
                                                    </Button>
                                                </TableCell>
                                                }
                                            </TableRow>
                                        ))}
                                    </TableBody>
                                </Table>
                            </TableContainer>
                            <br/><br/>
                            {/*  ----------- PENDING ORDERS TABLE ------------*/}
                            <Typography component="h1" variant="h5">Pending Orders</Typography><br/>
                            <TableContainer className={classes.table} component={Paper}
                                            style={{maxHeight: "17rem", overflow: "auto"}}>
                                <Table sx={{minWidth: 650}} size="small" aria-label="simple table">
                                    <TableHead>
                                        <TableRow>
                                            <TableCell>Initiating Party</TableCell>
                                            <TableCell align="left">Order Type</TableCell>
                                            <TableCell align="left">Type</TableCell>
                                            <TableCell align="left">Volume</TableCell>
                                            <TableCell align="left">Stock</TableCell>
                                            <TableCell align="left">Price</TableCell>
                                            <TableCell align="left">Expiration Date</TableCell>
                                            <TableCell align="left">Trade status</TableCell>
                                            <TableCell align="left">Trade ID</TableCell>
                                            <TableCell align="left"></TableCell>
                                        </TableRow>
                                    </TableHead>

                                    <TableBody>
                                        {this.state.trades.map((row, index) => (
                                            <TableRow key={index}
                                                      sx={{'&:last-child td, &:last-child th': {border: 0}}}>
                                                {row.map((item, idx) =>
                                                    // Display only pending trades
                                                    (row[8] === "Pending") && (item !== "null") && (row[2] === "Pending Order") &&
                                                    (<TableCell key={idx} component="th" scope="row">{item}</TableCell>)
                                                )}
                                                {/* Initiating Party cannot counter trade their trade */}
                                                {(row[8] === "Pending") && (!row[0].includes(this.getPartyfromPort())) && (row[2] === "Pending Order") &&
                                                <TableCell component="th" scope="row">
                                                    <Button
                                                        size='small'
                                                        style={{marginBottom: 5}}
                                                        type="submit"
                                                        fullWidth
                                                        variant="contained"
                                                        color="primary"
                                                        onClick={() => this.counterTradeButton(index, row[0])}>Accept
                                                    </Button>
                                                    {/*<br/>*/}
                                                    {/*<Button*/}
                                                    {/*    type="submit"*/}
                                                    {/*    fullWidth*/}
                                                    {/*    variant="contained"*/}
                                                    {/*    color="error"*/}
                                                    {/*    onClick={this.cancelTradeButton}>*/}
                                                    {/*    Reject Trade*/}
                                                    {/*</Button>*/}
                                                </TableCell>
                                                }
                                            </TableRow>
                                        ))}
                                    </TableBody>
                                </Table>
                            </TableContainer>
                            <br/><br/>
                        </div>
                    </Container>
                    <br/><br/>
                </div>
            </div>
        );
    }
}

export default withStyles(useStyles)(CreateTrade);