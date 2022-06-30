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

const drawerWidth = 290;
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
            value: "1",
            trades: [],
            nodes: [],
            peers: [],
            counterParty: "null",
            sellBuyValue: 0,
            sellBuyQuantity: 0,
            cashAmount: 1,
            stockAmount: 1,
            stockName: null,
            response: null,
            balance: 0,
            stockBalanceList: [],
            stockNames: [],
            stockToTrade: null,
            alignment: "sell"
        }
    }

    stockAmountChange = (e) => {
        this.setState({stockAmount: e.target.value});
    }
    stockNameChange = (e) => {
        this.setState({stockName: e.target.value});
    }
    cashAmountChange = (e) => {
        this.setState({cashAmount: e.target.value});
    }
    sellBuyValueChange = (e) => {
        this.setState({sellBuyValue: e.target.value});
    }
    stockToTradeChange = (e) => {
        this.setState({stockToTrade: e.target.value});
    }
    sellBuyQuantityChange = (e) => {
        this.setState({sellBuyQuantity: e.target.value});
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
        const data = {
            amount: parseFloat(this.state.cashAmount).toFixed(2)
        }

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
            this.setState({trades});
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

    counterTradeButton = (index, initiatingParty) => {
        let partyTrades = this.state.trades;

        const data = {
            initiatingParty: initiatingParty,
            counterParty: this.state.counterParty,
            sellValue: partyTrades[index][2],
            sellQuantity: partyTrades[index][3],
            buyValue: partyTrades[index][4],
            buyQuantity: partyTrades[index][5],
            stockToTrade: partyTrades[index][6],
            tradeStatus: "Accepted",
            tradeID: partyTrades[index][8],
        }

        if (data.sellQuantity !== "0") {

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
        } else if (data.buyQuantity !== "0") {

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


    buttonHandler = (e) => {
        e.preventDefault();

        if (this.state.sellBuyValue === 0 && this.state.sellBuyQuantity === 0) {
            window.alert("Cannot create trade with 0 values.")
        } else {

            let data = {};
            if (this.state.alignment === "sell") {
                data = {
                    counterParty: this.state.counterParty,
                    sellValue: parseFloat(this.state.sellBuyValue).toFixed(2),
                    sellQuantity: this.state.sellBuyQuantity,
                    buyValue: 0.0,
                    buyQuantity: 0,
                    stockToTrade: this.state.stockToTrade
                }
            } else if (this.state.alignment === "buy") {
                data = {
                    counterParty: this.state.counterParty,
                    sellValue: 0.0,
                    sellQuantity: 0,
                    buyValue: parseFloat(this.state.sellBuyValue).toFixed(2),
                    buyQuantity: this.state.sellBuyQuantity,
                    stockToTrade: this.state.stockToTrade
                }
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

    render() {
        const {classes} = this.props;
        return (
            <div>
                <CssBaseline/>
                <div className={classes.paper}>
                    <Container component="main" maxWidth="sm">

                        <Box sx={{display: 'flex'}}>
                            <CssBaseline/>
                            <AppBar position="fixed"
                                    sx={{width: `calc(100% - ${drawerWidth}px)`, ml: `${drawerWidth}px`}}> </AppBar>
                            <Drawer style={{fontSize: 15}} sx={{
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
                                            onChange={this.initiatingPartyChange}>
                                            {this.state.nodes.map((node, key) => (
                                                <MenuItem
                                                    key={key}
                                                    value={node}>{node}
                                                </MenuItem>))}
                                        </Select>
                                    </Grid>
                                </FormControl>
                                <br/>
                                <Divider/>

                                <form className={classes.form} style={{marginLeft: "20px", fontSize: 15}}
                                      id="createTradeForm" noValidate>
                                    <p style={{fontSize: 17, marginTop: "-5px"}}>Create a trade</p>

                                    <ToggleButtonGroup
                                        color="primary"
                                        value={this.state.alignment}
                                        exclusive
                                        onChange={this.toggleHandleChange}>
                                        <ToggleButton value="sell" style={{fontSize: 13}}>Sell</ToggleButton>
                                        <ToggleButton value="buy" style={{fontSize: 13}}>Buy</ToggleButton>
                                    </ToggleButtonGroup>
                                    <br/>

                                    <Grid container>
                                        <Grid item xs={10}>
                                            <FormControl required fullWidth>
                                                <InputLabel id="demo-simple-select-label" style={{fontSize: 15}}>Stock
                                                    Name</InputLabel>
                                                <Select
                                                    // value={}
                                                    defaultValue={''}
                                                    labelId="demo-simple-select-label"
                                                    id="demo-simple-select"
                                                    label="stockToTrade"
                                                    onChange={this.stockToTradeChange}>
                                                    {this.state.stockNames.map((stock, key) => (
                                                        <MenuItem style={{fontSize: 15}} key={key}
                                                                  value={stock}>{stock} </MenuItem>))}
                                                </Select>
                                            </FormControl>
                                        </Grid>
                                        <br/><br/><br/>
                                        <Grid item xs={10}>
                                            <TextField
                                                autoComplete="fname"
                                                name="sellBuyValue"
                                                variant="outlined"
                                                required
                                                fullWidth
                                                id="sellBuyValue"
                                                label="Stock Value (GBP)"
                                                InputProps={{style: {fontSize: 15}}}
                                                InputLabelProps={{style: {fontSize: 15}}}
                                                placeholder=""
                                                onChange={this.sellBuyValueChange}
                                                error={this.state.sellBuyValue === ""}
                                                helperText={this.state.sellBuyValue === "" ? 'Empty field!' : ' '}/>
                                        </Grid>
                                        <Grid item xs={10}>
                                            <TextField
                                                variant="outlined"
                                                required
                                                fullWidth
                                                id="sellBuyQuantity"
                                                label="Stock Quantity"
                                                name="sellBuyQuantity"
                                                autoComplete="sellBuyQuantity"
                                                InputProps={{style: {fontSize: 15}}}
                                                InputLabelProps={{style: {fontSize: 15}}}
                                                placeholder=""
                                                onChange={this.sellBuyQuantityChange}
                                                error={this.sellBuyQuantity === ""}
                                                helperText={this.sellBuyQuantity === "" ? 'Empty field!' : ' '}/>
                                        </Grid>
                                        <Grid item xs={10}>
                                            <Button style={{fontSize: 13}}
                                                    type="submit"
                                                    fullWidth
                                                    variant="contained"
                                                    color="primary"
                                                    onClick={this.buttonHandler}>
                                                Create Trade
                                            </Button>
                                        </Grid>
                                    </Grid>
                                </form>

                                <br/>
                                <Divider/>

                                <form className={classes.form} style={{marginLeft: "20px"}} id="issueMoneyForm"
                                      noValidate>
                                    <Grid item xs={10}>
                                        <TextField
                                            name="cashAmount"
                                            variant="outlined"
                                            fullWidth
                                            id="cashAmount"
                                            label="Amount (GBP)"
                                            InputProps={{style: {fontSize: 15}}}
                                            InputLabelProps={{style: {fontSize: 15}}}
                                            placeholder=""
                                            onChange={this.cashAmountChange}
                                            error={this.state.cashAmount === ""}
                                            helperText={this.state.cashAmount === "" ? 'Empty field!' : ' '}

                                        />
                                    </Grid>
                                    <Grid item xs={10}>
                                        <Button style={{fontSize: 13}}
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

                                <form className={classes.form} style={{marginLeft: "20px"}} id="issueStockForm"
                                      noValidate>

                                    <Grid container>
                                        <Grid item xs={10}>
                                            <TextField
                                                required
                                                name="stockName"
                                                variant="outlined"
                                                fullWidth
                                                id="stockName"
                                                label="Stock Name"
                                                InputProps={{style: {fontSize: 15}}}
                                                InputLabelProps={{style: {fontSize: 15}}}
                                                placeholder=""
                                                onChange={this.stockNameChange}
                                                error={this.state.stockName === ""}
                                                helperText={this.state.stockName === "" ? 'Empty field!' : ' '}
                                            />
                                        </Grid>
                                        <Grid item xs={10}>
                                            <TextField
                                                required
                                                name="stockAmount"
                                                variant="outlined"
                                                fullWidth
                                                id="stockAmount"
                                                label="Stock Amount"
                                                placeholder=""
                                                onChange={this.stockAmountChange}
                                                error={this.state.stockAmount === ""}
                                                helperText={this.state.stockAmount === "" ? 'Empty field!' : ' '}
                                            />
                                        </Grid>
                                        <Grid item xs={10}>
                                            <Button style={{fontSize: 13}}
                                                    fullWidth
                                                    type="submit"
                                                    variant="contained"
                                                    color="primary"
                                                    onClick={this.issueStock}>
                                                Issue Stocks
                                            </Button>
                                        </Grid></Grid>
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
                            <Drawer style={{fontSize: 15}} sx={{
                                width: drawerWidth, flexShrink: 0, '& .MuiDrawer-paper': {
                                    width: drawerWidth, boxSizing: 'border-box',
                                },
                            }} variant="permanent" anchor="right">
                                <Toolbar/>
                                {/*<Container component="main" maxWidth="sm">*/}
                                {/*    <TableContainer className={classes.table} component={Paper}>*/}
                                {/*        <Table sx={{minWidth: 250}} aria-label="simple table">*/}
                                {/*            <TableHead>*/}
                                {/*                <TableRow>*/}
                                {/*                    <TableCell align="center" style={{*/}
                                {/*                        fontWeight: 'bold',*/}
                                {/*                        fontSize: 20*/}
                                {/*                    }}>Stocks</TableCell>*/}
                                {/*                </TableRow>*/}
                                {/*            </TableHead>*/}
                                {/*            <TableBody>*/}
                                {/*                {this.state.stockBalanceList.map((row, index) => (*/}
                                {/*                    <TableRow key={index}*/}
                                {/*                              sx={{'&:last-child td, &:last-child th': {border: 0}}}>*/}
                                {/*                        <TableCell align="center" key={index} component="th"*/}
                                {/*                                   scope="row">{row}</TableCell>*/}
                                {/*                    </TableRow>*/}
                                {/*                ))}*/}
                                {/*            </TableBody>*/}
                                {/*        </Table>*/}
                                {/*    </TableContainer>*/}
                                {/*</Container>*/}

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
                                    <ListItem key={0}>
                                        <h3 style={{marginLeft: "4px", fontSize: 17}}>No issued stocks</h3>
                                    </ListItem>}
                                <Divider/>
                            </Drawer>
                        </Box>
                    </Container>


                    {/* ------------- Trades ------------- */}
                    <Container component="main" maxWidth="lg">
                        <CssBaseline/>
                        <div className={classes.paper}>
                            <Typography component="h1" variant="h4">
                                Trade History
                            </Typography>
                            <br/><br/>
                            <TableContainer className={classes.table} component={Paper}>
                                <Table sx={{minWidth: 650}} aria-label="simple table">
                                    <TableHead>
                                        <TableRow>
                                            <TableCell>Initiating Party</TableCell>
                                            <TableCell>Counter Party</TableCell>
                                            <TableCell align="left">Sell Value</TableCell>
                                            <TableCell align="left">Sell Amount</TableCell>
                                            <TableCell align="left">Buy Value</TableCell>
                                            <TableCell align="left">Buy Amount</TableCell>
                                            <TableCell align="left">Stock</TableCell>
                                            <TableCell align="left">Trade status</TableCell>
                                            <TableCell align="left">Trade ID</TableCell>
                                        </TableRow>
                                    </TableHead>
                                    <TableBody>
                                        {this.state.trades.map((row, index) => (
                                            <TableRow key={index}
                                                      sx={{'&:last-child td, &:last-child th': {border: 0}}}>
                                                {row.map((item, idx) => {
                                                    // Display only accepted trades
                                                    if (row[7] === "Accepted") {
                                                        return <TableCell key={idx} component="th"
                                                                          scope="row">{item}</TableCell>
                                                    }
                                                })}
                                            </TableRow>
                                        ))}
                                    </TableBody>
                                </Table>
                            </TableContainer>
                            <br/><br/>
                            <Typography component="h1" variant="h4">
                                Trades
                            </Typography>
                            <br/><br/>
                            <TableContainer className={classes.table} component={Paper}>
                                <Table sx={{minWidth: 650}} aria-label="simple table">
                                    <TableHead>
                                        <TableRow>
                                            <TableCell>Initiating Party</TableCell>
                                            <TableCell align="left">Sell Value</TableCell>
                                            <TableCell align="left">Sell Amount</TableCell>
                                            <TableCell align="left">Buy Value</TableCell>
                                            <TableCell align="left">Buy Amount</TableCell>
                                            <TableCell align="left">Stock</TableCell>
                                            <TableCell align="left">Trade status</TableCell>
                                            <TableCell align="left">Trade ID</TableCell>
                                            <TableCell align="left"></TableCell>
                                        </TableRow>
                                    </TableHead>

                                    <TableBody>
                                        {this.state.trades.map((row, index) => (
                                            <TableRow key={index}
                                                      sx={{'&:last-child td, &:last-child th': {border: 0}}}>
                                                {row.map((item, idx) => {
                                                    // Display only pending trades
                                                    if (row[7] === "Pending" && item !== "null") {
                                                        return <TableCell key={idx} component="th"
                                                                          scope="row">{item}</TableCell>
                                                    }
                                                })}
                                                {/* Initiating Party cannot counter trade their trade */}
                                                {(row[7] === "Pending") && (!row[0].includes(this.getPartyfromPort())) &&
                                                <TableCell component="th" scope="row">
                                                    <Button style={{marginBottom: 10}}
                                                            type="submit"
                                                            fullWidth
                                                            variant="contained"
                                                            color="primary"
                                                            onClick={() => this.counterTradeButton(index, row[0])}>
                                                        Accept
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