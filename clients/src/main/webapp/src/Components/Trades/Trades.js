import React, {Component} from 'react';
import CssBaseline from '@material-ui/core/CssBaseline';
import Typography from '@material-ui/core/Typography';
import {withStyles} from '@material-ui/core/styles';
import Container from '@material-ui/core/Container';
import axios from 'axios';
import corda_img from '../img/corda_img.png';
import {URL} from "../CONSTANTS";
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import Paper from '@mui/material/Paper';

const useStyles = (theme) => ({
    paper: {
        marginTop: theme.spacing(8),
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
    }
});

class Trades extends Component {
    constructor(props) {
        super(props);
        this.state = {
            trades: [],
        }
    }

    async componentDidMount() {

        // Initialise to PartyA port
        if (localStorage.getItem('port') === null) {
            localStorage.setItem('port', '10056');
        }
        this.getTrades();
    }

    getTrades() {
        let PORT = localStorage.getItem('port');
        axios.get(URL + PORT + "/trades", {
            headers: {
                'Access-Control-Allow-Origin': '*',
                'Content-Type': 'application/json'
            }
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

    render() {
        const {classes} = this.props;
        return (
            <Container component="main" maxWidth="lg">
                <CssBaseline/>
                <div className={classes.paper}>

                    <h2>{localStorage.getItem('currentNode')}</h2>

                    <img src={corda_img} alt="corda logo"/>

                    <Typography component="h1" variant="h2">
                        Trades
                    </Typography>
                    <br/><br/>

                    <TableContainer className={classes.table} component={Paper}>
                        <Table sx={{minWidth: 650}} aria-label="simple table">
                            <TableHead>
                                <TableRow>
                                    <TableCell>Initiating Party</TableCell>
                                    <TableCell align="left">Counter Party</TableCell>
                                    <TableCell align="left">Sell Value</TableCell>
                                    <TableCell align="left">Buy Value</TableCell>
                                    <TableCell align="left">Trade status</TableCell>
                                    <TableCell align="left">Trade ID</TableCell>
                                </TableRow>
                            </TableHead>

                            <TableBody>
                                {this.state.trades.map((row, index) => (
                                    <TableRow key={index} sx={{'&:last-child td, &:last-child th': {border: 0}}}>
                                        {row.map((item, idx) => {
                                            if (row[0].includes(this.getPartyfromPort())) {
                                                return <TableCell key={idx} component="th"
                                                                  scope="row">{item}</TableCell>
                                            }
                                        })}
                                    </TableRow>
                                ))}
                            </TableBody>
                        </Table>
                    </TableContainer>

                </div>
            </Container>
        );
    }
}

export default withStyles(useStyles)(Trades);