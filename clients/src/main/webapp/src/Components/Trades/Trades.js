// import React, {Component} from 'react';
// import CssBaseline from '@material-ui/core/CssBaseline';
// import Typography from '@mui/material/Typography';
// import {withStyles} from '@material-ui/core/styles';
// import Container from '@material-ui/core/Container';
// import axios from 'axios';
// import corda_img from '../img/corda_img.png';
// import {URL} from "../CONSTANTS";
// import Table from '@mui/material/Table';
// import TableBody from '@mui/material/TableBody';
// import TableCell from '@mui/material/TableCell';
// import TableContainer from '@mui/material/TableContainer';
// import TableHead from '@mui/material/TableHead';
// import TableRow from '@mui/material/TableRow';
// import Paper from '@mui/material/Paper';
// import Button from '@mui/material/Button';
// import * as PropTypes from "prop-types";
// import Box from "@mui/material/Box";
// import AppBar from "@mui/material/AppBar";
// import Toolbar from "@mui/material/Toolbar";
//
// const useStyles = (theme) => ({
//     paper: {
//         marginTop: theme.spacing(8),
//         display: 'flex',
//         flexDirection: 'column',
//         alignItems: 'center',
//     }
// });
//
// function View(props) {
//     return null;
// }
//
// View.propTypes = {
//     style: PropTypes.shape({flex: PropTypes.number, marginBottom: PropTypes.number}),
//     children: PropTypes.func
// };
//
// class Trades extends Component {
//
//     constructor(props) {
//         super(props);
//         this.state = {
//             trades: [],
//             balance: 0,
//             stockBalanceList: [],
//         }
//     }
//
//     async componentDidMount() {
//
//         // Initialise to PartyA port
//         if (localStorage.getItem('port') === null) {
//             localStorage.setItem('port', '10056');
//         }
//         this.getStockQuantity();
//         this.getBalance();
//         this.getCounterParty();
//         this.getTrades();
//     }
//
//     getStockQuantity() {
//         let PORT = localStorage.getItem('port');
//         axios.get(URL + PORT + "/getStockList", {
//             headers: {
//                 'Access-Control-Allow-Origin': '*',
//                 'Content-Type': 'application/json'
//             }
//         }).then(res => {
//             console.log(res.data)
//             let stockBalanceList = res.data;
//             if (stockBalanceList.length !== 0) {
//                 this.setState({stockBalanceList: res.data.Response});
//             }
//         });
//     }
//
//     getBalance() {
//         let PORT = localStorage.getItem('port');
//         axios.get(URL + PORT + "/getMoneyBalance", {
//             headers: {
//                 'Access-Control-Allow-Origin': '*',
//                 'Content-Type': 'application/json'
//             }
//         }).then(res => {
//             this.setState({balance: res.data.Amount});
//         });
//     }
//
//     getCounterParty() {
//         let PORT = localStorage.getItem('port');
//         axios.get(URL + PORT + "/node", {
//             headers: {
//                 'Access-Control-Allow-Origin': '*',
//                 'Content-Type': 'application/json'
//             }
//         }).then(res => {
//             let counterParty = res.data.name;
//             this.setState({counterParty})
//         }).catch(e => {
//             console.log(e);
//         });
//     }
//
//     getTrades() {
//         let PORT = localStorage.getItem('port');
//         axios.get(URL + PORT + "/trades", {
//             headers: {
//                 'Access-Control-Allow-Origin': '*',
//                 'Content-Type': 'application/json'
//             }
//         }).then(res => {
//             let trades = res.data;
//
//             trades.forEach(function (item, index) {
//                 trades[index] = item.split("|");
//             });
//             this.setState({trades});
//         }).catch(e => {
//             console.log(e);
//         });
//     }
//
//     getPartyfromPort() {
//         if (localStorage.getItem('port') === '10056') {
//             return 'PartyA'
//         } else if (localStorage.getItem('port') === '10057') {
//             return 'PartyB'
//         }
//     }
//
//     counterTradeButton = (index, initiatingParty) => {
//         let partyTrades = this.state.trades;
//
//         const data = {
//             initiatingParty: initiatingParty,
//             counterParty: this.state.counterParty,
//             sellValue: partyTrades[index][2],
//             sellQuantity: partyTrades[index][3],
//             buyValue: partyTrades[index][4],
//             buyQuantity: partyTrades[index][5],
//             stockToTrade: partyTrades[index][6],
//             tradeStatus: "Accepted",
//             tradeID: partyTrades[index][8],
//         }
//
//         if (data.sellQuantity !== "0") {
//
//             // find port of initiating party
//             let PORT;
//             if (data.initiatingParty.includes("PartyA")) {
//                 PORT = "10056";
//             } else if (data.initiatingParty.includes("PartyB")) {
//                 PORT = "10057";
//             }
//             console.log("ayee", data.sellQuantity, PORT);
//             // initiating party calls to move stocks from initiating party to counterparty
//             axios.post(URL + PORT + '/counterTrade', data, {
//                 headers: {
//                     'Access-Control-Allow-Origin': '*',
//                     'Content-Type': 'application/json'
//                 }
//             }).then(res => {
//                 console.log(res.data.Response);
//                 window.location.reload();
//             })
//
//
//         } else if (data.buyQuantity !== "0") {
//             console.log("here", localStorage.getItem('port') )
//             // counterparty calls to move stocks from counterparty to initiating party
//             let PORT = localStorage.getItem('port');
//             axios.post(URL + PORT + '/counterTrade', data, {
//                 headers: {
//                     'Access-Control-Allow-Origin': '*',
//                     'Content-Type': 'application/json'
//                 }
//             }).then(res => {
//                 console.log(res.data.Response);
//                 window.location.reload();
//             })
//
//         }
//
//
//
//         // let PORT = localStorage.getItem('port');
//         // axios.post(URL + PORT + '/counterTrade', data, {
//         //     headers: {
//         //         'Access-Control-Allow-Origin': '*',
//         //         'Content-Type': 'application/json'
//         //     }
//         // }).then(res => {
//         //     console.log(res.data.Response)
//         //     window.location.reload();
//         // })
//
//
//     }
//
//     // cancelTradeButton() {
//     //
//     // }
//
//     render() {
//         const {classes} = this.props;
//         return (
//             <div>
//                 <Container component="main" maxWidth="lg">
//                     <Box sx={{flexGrow: 1}}>
//                         <AppBar position="static">
//                             <Toolbar style={{marginRight: "20px"}}>
//                                 <img src={corda_img} style={{width: "100px"}} alt="corda logo"/>
//                                 <Typography variant="h7" component="div" sx={{flexGrow: 1}}>
//                                     <h2>{localStorage.getItem('currentNode')}</h2>
//                                 </Typography>
//                                 <h3>
//                                     Balance: <br/> {this.state.balance} GBP <br/>
//                                     Stocks: <br/> {this.state.stockBalanceList}
//                                 </h3>
//                             </Toolbar>
//                         </AppBar>
//                     </Box>
//                 </Container>
//
//             <Container component="main" maxWidth="lg">
//                 <CssBaseline/>
//                 <div className={classes.paper}>
//
//                     <Typography component="h1" variant="h3">
//                         Trade History
//                     </Typography>
//                     <br/><br/>
//
//                     <TableContainer className={classes.table} component={Paper}>
//                         <Table sx={{minWidth: 650}} aria-label="simple table">
//                             <TableHead>
//                                 <TableRow>
//                                     <TableCell>Initiating Party</TableCell>
//                                     <TableCell>Counter Party</TableCell>
//                                     <TableCell align="left">Sell Value</TableCell>
//                                     <TableCell align="left">Sell Amount</TableCell>
//                                     <TableCell align="left">Buy Value</TableCell>
//                                     <TableCell align="left">Buy Amount</TableCell>
//                                     <TableCell align="left">Trade status</TableCell>
//                                     <TableCell align="left">Trade ID</TableCell>
//                                 </TableRow>
//                             </TableHead>
//
//                             <TableBody>
//                                 {this.state.trades.map((row, index) => (
//                                     <TableRow key={index} sx={{'&:last-child td, &:last-child th': {border: 0}}}>
//                                         {row.map((item, idx) => {
//                                             // Display only accepted trades
//                                             if (row[6] === "Accepted") {
//                                                 return <TableCell key={idx} component="th"
//                                                                   scope="row">{item}</TableCell>
//                                             }
//                                         })}
//                                     </TableRow>
//                                 ))}
//                             </TableBody>
//                         </Table>
//                     </TableContainer>
//                     <br/><br/>
//
//                     <Typography component="h1" variant="h3">
//                         Trades
//                     </Typography>
//                     <br/><br/>
//
//                     <TableContainer className={classes.table} component={Paper}>
//                         <Table sx={{minWidth: 650}} aria-label="simple table">
//                             <TableHead>
//                                 <TableRow>
//                                     <TableCell>Initiating Party</TableCell>
//                                     <TableCell align="left">Sell Value</TableCell>
//                                     <TableCell align="left">Sell Amount</TableCell>
//                                     <TableCell align="left">Buy Value</TableCell>
//                                     <TableCell align="left">Buy Amount</TableCell>
//                                     <TableCell align="left">Stock Name</TableCell>
//                                     <TableCell align="left">Trade status</TableCell>
//                                     <TableCell align="left">Trade ID</TableCell>
//                                     <TableCell align="left"></TableCell>
//                                 </TableRow>
//                             </TableHead>
//
//                             <TableBody>
//                                 {this.state.trades.map((row, index) => (
//                                     <TableRow key={index} sx={{'&:last-child td, &:last-child th': {border: 0}}}>
//                                         {row.map((item, idx) => {
//                                             // Display only pending trades
//                                             if (row[6] === "Pending" && item !== "null") {
//                                                 return  <TableCell key={idx} component="th"
//                                                                   scope="row">{item}</TableCell>
//                                             }
//                                         })}
//                                         {/* Initiating Party cannot counter trade their trade */}
//                                         {(row[6] === "Pending") && (!row[0].includes(this.getPartyfromPort())) &&
//                                         <TableCell component="th" scope="row">
//                                             <Button style={{marginBottom: 10}}
//                                                     type="submit"
//                                                     fullWidth
//                                                     variant="contained"
//                                                     color="primary"
//                                                     onClick={() => this.counterTradeButton(index, row[0])}>
//                                                 Accept
//                                             </Button>
//                                             {/*<br/>*/}
//                                             {/*<Button*/}
//                                             {/*    type="submit"*/}
//                                             {/*    fullWidth*/}
//                                             {/*    variant="contained"*/}
//                                             {/*    color="error"*/}
//                                             {/*    onClick={this.cancelTradeButton}>*/}
//                                             {/*    Reject Trade*/}
//                                             {/*</Button>*/}
//                                         </TableCell>
//                                         }
//                                     </TableRow>
//                                 ))}
//                             </TableBody>
//                         </Table>
//                     </TableContainer>
//                     <br/><br/>
//                 </div>
//             </Container>
//             </div>
//         );
//     }
// }
//
// export default withStyles(useStyles)(Trades);