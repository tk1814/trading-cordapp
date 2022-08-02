import React, {Component} from 'react';
import CssBaseline from '@material-ui/core/CssBaseline';
import TextField from '@material-ui/core/TextField';
import Grid from '@material-ui/core/Grid';
import {withStyles} from '@material-ui/core/styles';
import Container from '@material-ui/core/Container';
import axios from 'axios';
import corda_img from './img/corda_img.png';
import {FormControl, InputAdornment, InputLabel, MenuItem, Select} from "@material-ui/core";
import {ToggleButton, ToggleButtonGroup} from "@material-ui/lab";
import AppBar from '@mui/material/AppBar';
import Box from '@mui/material/Box';
import Toolbar from '@mui/material/Toolbar';
import Button from '@mui/material/Button';
import Drawer from '@mui/material/Drawer';
import List from '@mui/material/List';
import Divider from '@mui/material/Divider';
import ListItem from '@mui/material/ListItem';
import ListItemText from '@mui/material/ListItemText';
import IssueStocks from "./IssueStocks";
import IssueMoney from "./IssueMoney";
import TradesTables from "./TradesTables";
import {timezone, location, URL, headers, parties} from "./CONSTANTS"

const drawerWidth = 300;
const useStyles = (theme) => ({
    paper: {
        marginTop: theme.spacing(7), display: 'flex', flexDirection: 'column', alignItems: 'center',
    },
    form: {
        marginTop: theme.spacing(3), width: '100%', // Fix IE 11 issue.
    },
    submit: {margin: theme.spacing(3, 0, 2),},
});

class CreateTrade extends Component {
    constructor(props) {
        super(props);
        this.state = {
            orderType: "pendingOrder",
            trades: [],
            nodes: [],
            counterParty: "null",
            stockPrice: 0,
            stockQuantity: 0,
            stockAmountToIssue: 1,
            moneyAmountToIssue: 1,
            stockNameToIssue: null,
            response: null,
            balance: 0,
            stockBalanceList: [],
            stockNames: [],
            stockToTrade: null,
            alignment: "sell",
            bidPrice: "--",
            askPrice: "--",
            intervalId: null,
            stockCodes: ["AAPL", "TSLA", "NFLX", "META", "GOOG", "TWTR"],
        }
    }

    stockAmountToIssueChange = (e) => {
        this.setState({stockAmountToIssue: e.target.value});
    }
    stockNameToIssueChange = (e) => {
        this.setState({stockNameToIssue: e.target.value});
    }
    moneyAmountToIssueChange = (e) => {
        this.setState({moneyAmountToIssue: e.target.value});
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
        // Initialise to first party port
        if (localStorage.getItem('port') === null) {
            let first = Object.values(parties)[0]
            localStorage.setItem('port', first);
        }
        if (await this.getAllNodes()) {
            this.getBalance();
            this.getStockQuantity();
            this.getCounterParty();
            this.getTrades();

            let formattedDate = await this.getDateTime(false);
            this.setState({expirationDate: formattedDate})
            this.setState({minFormattedDate: formattedDate})

            const interval = setInterval(() => {
                this.getBalance();
                this.getStockQuantity();
                this.getTrades();
            }, 2000);
            return () => clearInterval(interval) // clearInterval when the component is unmounted
        }
    }

    issueStock = (e) => {
        e.preventDefault()
        if (this.state.stockNames.includes(this.state.stockNameToIssue))
            window.alert("Cannot issue stock with an existing name.")
        else {
            const data = {
                amount: parseFloat(this.state.stockAmountToIssue).toFixed(0),
                name: this.state.stockNameToIssue
            }

            let PORT = localStorage.getItem('port');
            axios.post(URL + PORT + '/issueStock', data, {
                headers: headers
            }).then(res => {
                const response = res.data.Response;
                if (response !== null) {
                    if (response.includes("Success")) {
                        console.log(res.data.Name + " stocks issued: " + res.data.Amount + ".");
                    }
                }
            }).catch(e => {
                console.log(e);
            });
        }
    }

    issueMoney = (e) => {
        e.preventDefault()
        const data = {amount: parseFloat(this.state.moneyAmountToIssue).toFixed(2)}
        let PORT = localStorage.getItem('port');
        axios.post(URL + PORT + '/issueMoney', data, {
            headers: headers
        }).then(res => {
            const response = res.data.Response;
            if (response !== null) {
                if (response.includes("Success")) {
                    console.log("Cash issued: " + res.data.Amount);
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
            if (res.data.length !== 0) {
                let balances = [];
                let names = [];

                res.data[0].forEach(function (item, index) {
                    balances[index] = item.replace("=", " ")
                    names[index] = item.split("=")[1]
                });

                let stockBalanceList = balances.sort((a, b) => a.split(" ")[1] > b.split(" ")[1] ? 1 : -1);
                let stockNames = names.sort((a, b) => a > b ? 1 : -1);

                this.setState({stockBalanceList});
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
        let success = false;
        return axios.get(URL + PORT + "/nodes", {
            headers: headers
        }).then(res => {
            success = true;
            this.setState({nodes: res.data.nodes});
            return success;
        });

    }

    getCounterParty() {
        let PORT = localStorage.getItem('port');
        axios.get(URL + PORT + "/node", {
            headers: headers
        }).then(res => {
            this.setState({counterParty: res.data.name})
        }).catch(e => {
            console.log(e);
        });
    }

    getTrades() {
        let PORT = localStorage.getItem('port');
        axios.get(URL + PORT + "/trades", {
            headers: headers
        }).then(async res => {
                let trades = res.data;
                trades.forEach(function (item, index) {
                    trades[index] = JSON.parse(item);
                });
                let invertedTrades = trades.reverse();
                this.setState({trades: invertedTrades});
            }
        ).catch(e => {
            console.log(e);
        });
    }

    getPartyfromPort() {
        let currentPort = localStorage.getItem('port')
        return Object.keys(parties).find(key => parties[key] === parseInt(currentPort))
    }

    initiatingPartyChange = (e) => {
        let node = e.target.value;
        localStorage.setItem('currentNode', node);
        localStorage.setItem('port', parties[node]);

        // reloading the page triggers componentDidMount
        window.location.reload();
    }

    checkEnoughBalance = (tradeType, stockQuantity, stockToTrade, stockPrice, reverseOps) => {

        // get current node
        let party = this.state.counterParty;
        let committedMoneyBalance = 0.0;
        let committedStockBalance = 0;

        this.state.trades.forEach(function (trade, index) {
            // take current node's pending trades to buy
            if (trade.initiatingParty.includes(party) && trade.tradeStatus === "Pending" && trade.tradeType === "Buy") {
                committedMoneyBalance += parseFloat((parseFloat(trade.stockPrice) * parseFloat(trade.stockQuantity)).toFixed(2));
            }
            if (trade.initiatingParty.includes(party) && trade.tradeStatus === "Pending" && trade.tradeType === "Sell" && trade.stockName === stockToTrade) {
                committedStockBalance += parseInt(trade.stockQuantity);
            }
        });

        // get current stock balance
        let stockBalanceAndName = this.state.stockBalanceList.find((stockSymbol) => stockSymbol.includes(stockToTrade));

        let stockBalance = parseInt(stockBalanceAndName.split(" ")[0]);
        let moneyBalance = parseFloat(this.state.balance.trim().split(" ")[0]);

        if ((tradeType === "Sell" && reverseOps) || (tradeType === "Buy" && !reverseOps)) {

            // must have enough stocks to sell them
            if (parseInt(stockQuantity) > (stockBalance - committedStockBalance)) {
                window.alert("You don't have enough stocks to sell.\nAvailable uncommitted stocks: " + (stockBalance - committedStockBalance) + " " + stockToTrade + ".");
                return false;
            }
        } else if ((tradeType === "Buy" && reverseOps) || (tradeType === "Sell" && !reverseOps)) {

            // must have enough money balance to buy stocks
            let cost = parseFloat((parseFloat(stockPrice) * parseFloat(stockQuantity)).toFixed(2))

            if (moneyBalance === 0.0) {
                window.alert("You have 0 balance. You cannot buy stocks.")
                return false;
            } else if (cost > (moneyBalance - committedMoneyBalance)) {
                if (moneyBalance - committedMoneyBalance < 0)
                    window.alert("You don't have enough balance to buy stocks.")
                else
                    window.alert("You don't have enough balance to buy stocks.\nAvailable uncommitted balance: " + (moneyBalance - committedMoneyBalance).toFixed(2) + " USD.")
                return false;
            }
        }
        return true;
    }

    createTradeOrder = async (e, marketOrderType) => {
        e.preventDefault();

        if (this.state.stockQuantity === 0 || this.state.stockQuantity < 0) {
            window.alert("Cannot create trade with 0 or negative values.")
        } else {

            let isEnough, data = null;
            if (marketOrderType === null) { // PENDING ORDER

                let currentDateTimeString = await this.getDateTime(true);
                let currentDateTime = new Date(currentDateTimeString)
                let selectedDateTime = new Date(this.state.expirationDate);

                // don't allow previous expiration dates, if today's date is chosen as expiration date: don't allow previous times
                if (this.state.stockPrice === 0 || this.state.stockPrice < 0) {
                    window.alert("Cannot create trade with 0 or negative values.")
                } else if (this.state.expirationDate === null) {
                    window.alert("Null expiration date.")
                } else if (selectedDateTime.getDate() < currentDateTime.getDate()) {
                    window.alert("Invalid expiration day.")
                } else if (selectedDateTime.getMonth() < currentDateTime.getMonth()) {
                    window.alert("Invalid expiration month.")
                } else if (selectedDateTime.getFullYear() < currentDateTime.getFullYear()) {
                    window.alert("Invalid expiration year.")
                } else if (currentDateTimeString.slice(0, 10) === this.state.expirationDate.slice(0, 10) && selectedDateTime.getTime() < (currentDateTime.getTime() + (4 * 60000))) {
                    window.alert("Invalid expiration time. Expiration interval should be at least 5 minutes.")
                } else {
                    let tradeType = (this.state.alignment === "sell") ? "Sell" : "Buy";
                    isEnough = this.checkEnoughBalance(tradeType, this.state.stockQuantity, this.state.stockToTrade, this.state.stockPrice, true);
                    if (isEnough) {
                        data = {
                            orderType: "Pending Order",
                            tradeType: tradeType,
                            stockName: this.state.stockToTrade,
                            stockPrice: parseFloat(this.state.stockPrice).toFixed(2),
                            stockQuantity: this.state.stockQuantity,
                            expirationDate: this.state.expirationDate,
                            tradeDate: currentDateTimeString,
                        }
                    }
                }
            } else { // MARKET ORDER

                let stockPrice = (marketOrderType === "sellByMarket") ? this.state.askPrice : this.state.bidPrice;
                if (stockPrice === "--" || stockPrice === 0) {
                    window.alert("Invalid stock price.");
                } else {
                    let tradeType = (marketOrderType === "sellByMarket") ? "Sell" : "Buy";
                    isEnough = this.checkEnoughBalance(tradeType, this.state.stockQuantity, this.state.stockToTrade, stockPrice, true);

                    if (isEnough) {
                        let currentDateTimeString = await this.getDateTime(true);
                        let currentDateTime = new Date(currentDateTimeString + 'Z'); // add Z to give UTC time string
                        let newTime = currentDateTime.getTime() + 2 * 60000;

                        data = {
                            orderType: "Market Order",
                            tradeType: tradeType,
                            stockName: this.state.stockToTrade,
                            stockPrice: stockPrice,
                            stockQuantity: this.state.stockQuantity,
                            expirationDate: new Date(newTime).toISOString().slice(0, 16), // sets 2 minutes expiration date
                            tradeDate: currentDateTimeString
                        }
                    }
                }
            }
            if (data !== null && isEnough) {
                let PORT = localStorage.getItem('port');
                axios.post(URL + PORT + '/createTrade', data, {
                    headers: headers
                }).then(res => {
                    const response = res.data.Response;
                    if (response !== null) {
                        console.log(response);
                        this.setState({response});
                    }
                }).catch(e => {
                    console.log(e);
                });
            }
        }
    };

    counterTradeButton = async (index) => {
        let trades = this.state.trades;

        const data = {
            counterParty: this.state.counterParty,
            settlementDate: await this.getDateTime(true),
            tradeID: trades[index].linearId,
        }

        let isEnough = this.checkEnoughBalance(trades[index].tradeType, trades[index].stockQuantity, trades[index].stockName, trades[index].stockPrice, false);

        let currentDateTimeString = await this.getDateTime(false);
        let currentDateTime = new Date(currentDateTimeString)
        let selectedDateTime = new Date(trades[index].expirationDate);

        // check there is enough balance and trade has not expired
        if (isEnough && currentDateTime < selectedDateTime) {

            let PORT;
            if (trades[index].tradeType === "Sell") {
                // initiating party calls to move stocks from initiating party to counterparty
                PORT = parties[trades[index].initiatingParty] // get port from initiating party
            } else if (trades[index].tradeType === "Buy") {
                // counterparty calls to move stocks from counterparty to initiating party
                PORT = localStorage.getItem('port');
            }

            axios.post(URL + PORT + '/counterTrade', data, {
                headers: headers
            }).then(res => {
                console.log(res.data.Response);
            })
        }
    }

    cancelTradeButton = (index) => {

        let trades = this.state.trades;
        const data = {
            tradeStatus: "Cancelled",
            tradeID: trades[index].linearId,
        }
        let PORT = localStorage.getItem('port');
        axios.post(URL + PORT + '/cancelTrade', data, {
            headers: headers
        }).then(res => {
            console.log(res.data.Response);
        })
    }

    async getDateTime(withMilliseconds) {
        let formattedDate;
        const result = await fetch("https://worldtimeapi.org/api/timezone/" + location)
            .then(response => response.json())
            .then(data => {
                let datetime = data.utc_datetime;
                if (withMilliseconds) {
                    formattedDate = datetime.slice(0, 23)
                } else {
                    formattedDate = datetime.slice(0, 16)
                }
                return formattedDate
            })
        return await result
    }

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
                'x-api-key': 'LYYwjsR35b8BKuiqo22i986RSxSG7tEc8LDFY44A'
            }
        };

        axios.request(options).then(response => {
            if (response !== null) {
                let bidPrice = response.data.quoteResponse.result[0].bid;
                let askPrice = response.data.quoteResponse.result[0].ask;
                if (bidPrice !== 0 && askPrice !== 0) {
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

    render() {
        const {classes} = this.props;
        return (
            <div>
                <CssBaseline/>
                <div className={classes.paper}>
                    <Container component="main" maxWidth="sm">
                        <Box sx={{display: 'flex'}}> <CssBaseline/>
                            <AppBar position="fixed" sx={{
                                width: `calc(100% - ${drawerWidth}px)`,
                                ml: `${drawerWidth}px`
                            }}> </AppBar>
                            <Drawer sx={{
                                width: drawerWidth,
                                flexShrink: 0, '& .MuiDrawer-paper': {width: drawerWidth, boxSizing: 'border-box',},
                            }} variant="permanent" anchor="left">
                                <Toolbar> <img src={corda_img} style={{
                                    width: "80px",
                                    marginLeft: "30%"
                                }} alt="corda logo"/> </Toolbar>
                                <Divider/>
                                <br/>

                                <FormControl required fullWidth style={{marginLeft: "20px"}}>
                                    <Grid item xs={10}>
                                        <InputLabel id="demo-simple-select-label">Initiating Party</InputLabel>
                                        <Select value={localStorage.getItem('currentNode')}
                                                defaultValue={''} labelId="demo-simple-select-label"
                                                id="demo-simple-select" label="Party"
                                                onChange={this.initiatingPartyChange} fullWidth>
                                            {this.state.nodes.map((node, key) => (
                                                <MenuItem key={key} value={node}>{node}</MenuItem>))}
                                        </Select>
                                    </Grid>
                                </FormControl> <br/> <Divider/>

                                {/* ------------------ ORDERS ------------------------*/}
                                <Grid container spacing={2}>
                                    <Grid item xs={10} style={{marginLeft: "20px"}}>
                                        <FormControl required fullWidth>
                                            <InputLabel id="demo-simple-select-label">Stock</InputLabel>
                                            <Select
                                                // value={}
                                                defaultValue={''} labelId="demo-simple-select-label"
                                                id="demo-simple-select" label="stockToTrade"
                                                onChange={this.stockToTradeChange}>
                                                {this.state.stockNames.map((stock, key) => (
                                                    <MenuItem key={key} value={stock}>{stock} </MenuItem>))}
                                            </Select>
                                        </FormControl>
                                    </Grid>

                                    <Grid item xs={10} style={{marginLeft: "20px"}}>
                                        <FormControl required fullWidth>
                                            <Select
                                                value={this.state.orderType} defaultValue={"pendingOrder"}
                                                labelId="demo-simple-select-label" id="demo-simple-select"
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
                                                size="small" variant="outlined" required fullWidth id="stockQuantity"
                                                label="Volume" name="stockQuantity" type="number"
                                                autoComplete="stockQuantity" placeholder=""
                                                onChange={this.stockQuantityChange}
                                                error={this.state.stockQuantity === "" || this.state.stockQuantity < 0}
                                                helperText={this.state.stockQuantity === "" ? 'Empty field!' : ' '}/>
                                        </Grid>
                                        <Grid item xs={10}>
                                            <TextField
                                                size="small" autoComplete="fname" name="stockPrice" variant="outlined"
                                                required fullWidth id="stockPrice" label="Stock Price" type="number"
                                                InputProps={{
                                                    startAdornment: (
                                                        <InputAdornment position="start">$</InputAdornment>)
                                                }}
                                                placeholder="" onChange={this.stockPriceChange}
                                                error={this.state.stockPrice === ""}
                                                helperText={this.state.stockPrice === "" ? 'Empty field!' : ' '}/>
                                        </Grid>
                                        {(this.state.minFormattedDate) &&
                                        <Grid item xs={10}>
                                            <TextField
                                                fullWidth id="expirationDate" label={"Expiration Date " + timezone}
                                                inputProps={{
                                                    defaultValue: this.state.expirationDate,
                                                    min: this.state.minFormattedDate
                                                }} InputLabelProps={{shrink: true}}
                                                type="datetime-local" onChange={this.expirationDateChange}/>
                                        </Grid>}
                                        <br/><br/><br/>
                                        <Grid item xs={10}>
                                            <Button
                                                size="small" type="submit" fullWidth variant="contained" color="primary"
                                                onClick={(e) => this.createTradeOrder(e, null)}>Place Order</Button>
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
                                                size="small" variant="outlined" required fullWidth id="stockQuantity"
                                                label="Volume" name="stockQuantity" autoComplete="stockQuantity"
                                                placeholder="" type="number" onChange={this.stockQuantityChange}
                                                error={this.state.stockQuantity === "" || this.state.stockQuantity < 0}
                                                helperText={this.state.stockQuantity === "" ? 'Empty field!' : ' '}/>
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
                                                    size="small" type="submit" fullWidth variant="contained" color="primary"
                                                    onClick={(e) => this.createTradeOrder(e, "sellByMarket")}>
                                                    Sell by Market
                                                </Button>
                                            </Grid>
                                            <Grid item xs={10}>
                                                <Button
                                                    size="small" type="submit" fullWidth variant="contained" color="primary"
                                                    onClick={(e) => this.createTradeOrder(e, "buyByMarket")}>
                                                    Buy By Market
                                                </Button>
                                            </Grid>
                                        </Grid>
                                    </Grid>
                                </form>}

                                <br/>
                                <Divider/>

                                {/* -------------- ISSUE MONEY -------------- */}
                                <IssueMoney
                                    forms={classes.form} moneyAmountToIssueChange={this.moneyAmountToIssueChange}
                                    moneyAmountToIssue={this.state.moneyAmountToIssue}
                                    issueMoney={this.issueMoney}/> <br/> <Divider/>

                                {/* -------------- ISSUE STOCKS -------------- */}
                                <IssueStocks
                                    forms={classes.form} stockCodes={this.state.stockCodes}
                                    stockNameToIssueChange={this.stockNameToIssueChange}
                                    stockAmountToIssueChange={this.stockAmountToIssueChange}
                                    stockAmountToIssue={this.state.stockAmountToIssue}
                                    issueStock={this.issueStock}/> <br/>
                            </Drawer>
                        </Box>
                    </Container>


                    {/* ------------- Balance & Stocks ------------- */}
                    <Container component="main" maxWidth="sm">
                        <Box sx={{display: 'flex'}}> <CssBaseline/>
                            <AppBar position="fixed"
                                    sx={{width: `calc(100% - ${drawerWidth}px)`, ml: `${drawerWidth}px`}}> </AppBar>
                            <Drawer sx={{
                                width: drawerWidth, flexShrink: 0, '& .MuiDrawer-paper': {
                                    width: drawerWidth, boxSizing: 'border-box',
                                },
                            }} variant="permanent" anchor="right">
                                <Toolbar/>
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
                                    </List> : <ListItem key={0}><h3 style={{marginLeft: "4px", fontSize: 17}}>No issued
                                        stocks</h3></ListItem>}
                                <Divider/>
                            </Drawer>
                        </Box>
                    </Container>


                    {/* ------------- TRADES TABLES ------------- */}
                    <TradesTables
                        forms={classes.form} paper={classes.paper} table={classes.table}
                        trades={this.state.trades}
                        getPartyfromPort={this.getPartyfromPort}
                        cancelTradeButton={this.cancelTradeButton}
                        drawerWidth={drawerWidth}
                        counterTradeButton={this.counterTradeButton}
                        tz={timezone}/> <br/>
                </div>
            </div>
        );
    }
}

export default withStyles(useStyles)(CreateTrade);