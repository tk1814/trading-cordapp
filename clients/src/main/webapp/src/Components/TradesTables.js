import CssBaseline from "@material-ui/core/CssBaseline";
import Typography from "@mui/material/Typography";
import TableContainer from "@mui/material/TableContainer";
import Paper from "@mui/material/Paper";
import Table from "@mui/material/Table";
import TableHead from "@mui/material/TableHead";
import TableRow from "@mui/material/TableRow";
import TableCell from "@mui/material/TableCell";
import TableBody from "@mui/material/TableBody";
import Button from "@mui/material/Button";
import Container from "@material-ui/core/Container";
import React from "react";

const TradeTables = (props) => (

    <Container component="main" maxWidth="xl" style={{width: `calc(100% - 1.93*${props.drawerWidth}px)`}}>
        <CssBaseline/>
        <div className={props.paper} style={{marginTop: "-35px"}}>
            <Typography component="h1" variant="h5" style={{marginBottom: '5px'}}>Trade History</Typography>
            <TableContainer className={props.table} component={Paper} style={{maxHeight: "17rem", overflow: "auto"}}>
                <Table stickyHeader sx={{minWidth: 650}} size="small" aria-label="simple table">
                    <TableHead>
                        <TableRow>
                            <TableCell style={{fontWeight: 'bold'}}>Initiating Party</TableCell>
                            <TableCell style={{fontWeight: 'bold'}}>Counter Party</TableCell>
                            <TableCell style={{fontWeight: 'bold'}} align="left">Order Type</TableCell>
                            <TableCell style={{fontWeight: 'bold'}} align="left">Type</TableCell>
                            <TableCell style={{fontWeight: 'bold'}} align="left">Volume</TableCell>
                            <TableCell style={{fontWeight: 'bold'}} align="left">Symbol</TableCell>
                            <TableCell style={{fontWeight: 'bold'}} align="left">Price</TableCell>
                            <TableCell style={{fontWeight: 'bold'}} align="left">Trade Date {props.tz}</TableCell>
                            <TableCell style={{fontWeight: 'bold'}} align="left">Settlement Date {props.tz}</TableCell>
                            <TableCell style={{fontWeight: 'bold'}} align="left">Trade status</TableCell>
                            <TableCell style={{fontWeight: 'bold'}} align="left">Trade ID</TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>

                        {props.trades.map((row, index) => (
                            // Display only accepted trades
                            // don't show expiration date
                            // display only trades that the current node initiated or accepted
                            (row.tradeStatus === "Accepted" || row.tradeStatus === "Cancelled" || row.tradeStatus === "Expired") &&
                            (row.initiatingParty === props.getPartyfromPort()) &&
                            <TableRow key={index} sx={{'&:last-child td, &:last-child th': {border: 0}}}>
                                <TableCell key={0} component="th" scope="row">{row.initiatingParty}</TableCell>
                                {(row.counterParty === "null") ?
                                    <TableCell key={1} component="th" scope="row">N/A</TableCell> :
                                    <TableCell key={1} component="th" scope="row">{row.counterParty}</TableCell>}
                                <TableCell key={2} component="th" scope="row">{row.orderType}</TableCell>
                                <TableCell key={3} component="th" scope="row">{row.tradeType}</TableCell>
                                <TableCell key={4} component="th" scope="row">{row.stockQuantity}</TableCell>
                                <TableCell key={5} component="th" scope="row">{row.stockName}</TableCell>
                                <TableCell key={6} component="th" scope="row">{row.stockPrice}</TableCell>
                                <TableCell key={7} component="th" scope="row">{row.tradeDate}</TableCell>
                                {(row.settlementDate === "null") ?
                                    <TableCell key={8} component="th" scope="row">N/A</TableCell> :
                                    <TableCell key={8} component="th" scope="row">{row.settlementDate}</TableCell>}
                                <TableCell key={9} component="th" scope="row">{row.tradeStatus}</TableCell>
                                <TableCell key={10} component="th" scope="row">{row.linearId}</TableCell>
                            </TableRow>
                        ))}
                    </TableBody>
                </Table>
            </TableContainer>

            {/*  ----------- MARKET ORDERS TABLE ------------*/}
            <br/>
            <Typography component="h1" variant="h5" style={{marginBottom: '5px'}}>Market
                Orders</Typography>
            <TableContainer className={props.table} component={Paper} style={{maxHeight: "14rem", overflow: "auto"}}>
                <Table stickyHeader sx={{minWidth: 650}} size="small" aria-label="simple table">
                    <TableHead>
                        <TableRow>
                            <TableCell style={{fontWeight: 'bold'}}>Initiating Party</TableCell>
                            <TableCell style={{fontWeight: 'bold'}} align="left">Order Type</TableCell>
                            <TableCell style={{fontWeight: 'bold'}} align="left">Type</TableCell>
                            <TableCell style={{fontWeight: 'bold'}} align="left">Volume</TableCell>
                            <TableCell style={{fontWeight: 'bold'}} align="left">Symbol</TableCell>
                            <TableCell style={{fontWeight: 'bold'}} align="left">Price</TableCell>
                            <TableCell style={{fontWeight: 'bold'}} align="left">Trade Date {props.tz}</TableCell>
                            <TableCell style={{fontWeight: 'bold'}} align="left">Expiration Date {props.tz}</TableCell>
                            <TableCell style={{fontWeight: 'bold'}} align="left">Trade status</TableCell>
                            <TableCell style={{fontWeight: 'bold'}} align="left">Trade ID</TableCell>
                            <TableCell style={{fontWeight: 'bold'}} align="left"></TableCell>
                        </TableRow>
                    </TableHead>

                    <TableBody>
                        {props.trades.map((row, index) => (
                            // Display only pending trades
                            // Initiating Party cannot counter trade their trade
                            (row.tradeStatus === "Pending" && row.orderType === "Market Order") &&
                            <TableRow key={index} sx={{'&:last-child td, &:last-child th': {border: 0}}}>
                                <TableCell key={0} component="th" scope="row">{row.initiatingParty}</TableCell>
                                <TableCell key={1} component="th" scope="row">{row.orderType}</TableCell>
                                <TableCell key={2} component="th" scope="row">{row.tradeType}</TableCell>
                                <TableCell key={3} component="th" scope="row">{row.stockQuantity}</TableCell>
                                <TableCell key={4} component="th" scope="row">{row.stockName}</TableCell>
                                <TableCell key={5} component="th" scope="row">{row.stockPrice}</TableCell>
                                <TableCell key={6} component="th" scope="row">{row.tradeDate}</TableCell>
                                <TableCell key={7} component="th" scope="row">{row.expirationDate}</TableCell>
                                <TableCell key={8} component="th" scope="row">{row.tradeStatus}</TableCell>
                                <TableCell key={9} component="th" scope="row">{row.linearId}</TableCell>
                                {/*{(row.initiatingParty !== props.getPartyfromPort()) &&*/}
                                {/*<TableCell component="th" scope="row">*/}
                                {/*    <Button size='small' style={{marginBottom: 5}} type="submit"*/}
                                {/*            fullWidth variant="contained" color="primary"*/}
                                {/*            onClick={() => props.counterTradeButton(index)}>Accept </Button>*/}
                                {/*</TableCell>}*/}
                                {(row.initiatingParty === props.getPartyfromPort()) &&
                                <TableCell component="th" scope="row">
                                    <Button size='small' style={{marginBottom: 5}} type="submit"
                                            fullWidth variant="outlined" color="error"
                                            onClick={() => props.cancelTradeButton(index)}>Cancel</Button>
                                </TableCell>}
                            </TableRow>
                        ))}
                    </TableBody>
                </Table>
            </TableContainer>

            {/*  ----------- PENDING ORDERS TABLE ------------*/}
            <br/>
            <Typography component="h1" variant="h5" style={{marginBottom: '5px'}}>Pending
                Orders</Typography>
            <TableContainer className={props.table} component={Paper} style={{maxHeight: "17rem", overflow: "auto"}}>
                <Table stickyHeader sx={{minWidth: 650}} size="small" aria-label="simple table">
                    <TableHead>
                        <TableRow>
                            <TableCell style={{fontWeight: 'bold'}}>Initiating Party</TableCell>
                            <TableCell style={{fontWeight: 'bold'}} align="left">Order Type</TableCell>
                            <TableCell style={{fontWeight: 'bold'}} align="left">Type</TableCell>
                            <TableCell style={{fontWeight: 'bold'}} align="left">Volume</TableCell>
                            <TableCell style={{fontWeight: 'bold'}} align="left">Symbol</TableCell>
                            <TableCell style={{fontWeight: 'bold'}} align="left">Price</TableCell>
                            <TableCell style={{fontWeight: 'bold'}} align="left">Trade Date {props.tz}</TableCell>
                            <TableCell style={{fontWeight: 'bold'}} align="left">Expiration Date {props.tz}</TableCell>
                            <TableCell style={{fontWeight: 'bold'}} align="left">Trade status</TableCell>
                            <TableCell style={{fontWeight: 'bold'}} align="left">Trade ID</TableCell>
                            <TableCell style={{fontWeight: 'bold'}} align="left"></TableCell>
                        </TableRow>
                    </TableHead>

                    <TableBody>
                        {props.trades.map((row, index) => (
                            // Display only pending trades
                            // Initiating Party cannot counter trade their trade
                            (row.tradeStatus === "Pending" && row.orderType === "Pending Order") &&
                            <TableRow key={index} sx={{'&:last-child td, &:last-child th': {border: 0}}}>
                                <TableCell key={0} component="th" scope="row">{row.initiatingParty}</TableCell>
                                <TableCell key={1} component="th" scope="row">{row.orderType}</TableCell>
                                <TableCell key={2} component="th" scope="row">{row.tradeType}</TableCell>
                                <TableCell key={3} component="th" scope="row">{row.stockQuantity}</TableCell>
                                <TableCell key={4} component="th" scope="row">{row.stockName}</TableCell>
                                <TableCell key={5} component="th" scope="row">{row.stockPrice}</TableCell>
                                <TableCell key={8} component="th" scope="row">{row.tradeDate}</TableCell>
                                <TableCell key={6} component="th" scope="row">{row.expirationDate}</TableCell>
                                <TableCell key={7} component="th" scope="row">{row.tradeStatus}</TableCell>
                                <TableCell key={9} component="th" scope="row">{row.linearId}</TableCell>

                                {/*{(row.initiatingParty !== props.getPartyfromPort()) &&*/}
                                {/*<TableCell component="th" scope="row">*/}
                                {/*    <Button size='small' style={{marginBottom: 5}} type="submit"*/}
                                {/*            fullWidth variant="contained" color="primary"*/}
                                {/*            onClick={() => props.counterTradeButton(index)}>Accept</Button>*/}
                                {/*</TableCell>}*/}
                                {(row.initiatingParty === props.getPartyfromPort()) &&
                                <TableCell component="th" scope="row">
                                    <Button size='small' style={{marginBottom: 5}} type="submit"
                                            fullWidth variant="outlined" color="error"
                                            onClick={() => props.cancelTradeButton(index)}>Cancel</Button>
                                </TableCell>}
                            </TableRow>
                        ))}
                    </TableBody>
                </Table>
            </TableContainer>
        </div>
    </Container>

);

export default TradeTables;