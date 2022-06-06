import React, {useState, useEffect} from 'react';
import Button from '@material-ui/core/Button';
import CssBaseline from '@material-ui/core/CssBaseline';
import TextField from '@material-ui/core/TextField';
import Grid from '@material-ui/core/Grid';
import Typography from '@material-ui/core/Typography';
import {makeStyles} from '@material-ui/core/styles';
import Container from '@material-ui/core/Container';
import {useHistory, withRouter} from 'react-router-dom';
import axios from 'axios';

import corda_img from '../img/corda_img.png';
import {BACKEND_URL} from '../CONSTANTS.js';

// function validateEmail(email) {
//   const re = /^(([^<>()\[\]\\.,;:\s@"]+(\.[^<>()\[\]\\.,;:\s@"]+)*)|(".+"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/;
//   return re.test(String(email).toLowerCase());
// }

const useStyles = makeStyles((theme) => ({
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
}));

function CreateTrade(props) {

    // state variables and related functions
    const [counterParty, setCounterParty] = useState("O=PartyB, L=New York, C=US"); // initialise variables
    const [sellValue, setSellValue] = useState("1");
    const [sellCurrency, setSellCurrency] = useState("GBP");
    const [buyValue, setBuyValue] = useState("1");
    const [buyCurrency, setBuyCurrency] = useState("EUR");

    const counterPartyChange = e => setCounterParty(e.target.value);
    const sellValueChange = e => setSellValue(e.target.value);
    const sellCurrencyChange = e => setSellCurrency(e.target.value);
    const buyValueChange = e => setBuyValue(e.target.value);
    const buyCurrencyChange = e => setBuyCurrency(e.target.value);

    let inputData = [counterParty, sellValue, sellCurrency, buyValue, buyCurrency];

    const classes = useStyles();
    const history = useHistory();

    const [response, setResponse] = useState(null);

    function buttonHandler(e) {
        e.preventDefault();

        console.log(inputData);

        const data = {
            // inputData: inputData,
            counterParty: counterParty,
            sellValue: sellValue,
            sellCurrency: sellCurrency,
            buyValue: buyValue,
            buyCurrency: buyCurrency,
        }

        axios.post(
            // BACKEND_URL + '/games',
            BACKEND_URL + '/createTrade',
            data,
            {headers: {'Access-Control-Allow-Origin': '*', 'Content-Type': 'application/json'}}
        ).then(res => {

            // TODO SOMETHING WITH THE RESPONSE
            const response = res.data;
            console.log('RESPONSE:', response);
            if (response !== null) {
                setResponse(res);
            }

        }).catch(e => {
            console.log(e);
        });

    };

    // note for anyone curious how this useEffect binds to the response
    // https://stackoverflow.com/questions/63603966/react-api-call-with-axios-how-to-bind-an-onclick-event-with-an-api-call

    useEffect(() => {
        if (response !== null) {
            let path = "/created";
            history.push(path);
        }
    }, [response]); // eslint-disable-line react-hooks/exhaustive-deps

    return (
        <Container component="main" maxWidth="sm">
            <CssBaseline/>
            <div className={classes.paper}>

                <img src={corda_img} alt="corda logo"/>

                <Typography component="h1" variant="h2">
                    Trading CordApp
                </Typography>

                <form className={classes.form} id="createSantaForm" noValidate>

                    <Grid container spacing={2}>

                        <Grid item xs={12} sm={12}>
                            <TextField
                                autoComplete="fname"
                                name="counterParty"
                                variant="outlined"
                                required
                                fullWidth
                                id="counterParty"
                                //{/*^^firstName1*/}
                                label="Counter Party"
                                placeholder="O=PartyB, L=New York, C=US"
                                onChange={counterPartyChange}
                                error={counterParty === ""}
                                helperText={counterParty === "" ? 'Empty field!' : ' '}
                                autoFocus
                            />
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
                                onChange={sellValueChange}
                                error={sellValue === ""}
                                helperText={sellValue === "" ? 'Empty field!' : ' '}
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
                                onChange={sellCurrencyChange}
                                error={sellCurrency === ""}
                                helperText={sellCurrency === "" ? 'Empty field!' : ' '}
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
                                onChange={buyValueChange}
                                error={buyValue === ""}
                                helperText={buyValue === "" ? 'Empty field!' : ' '}
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
                                onChange={buyCurrencyChange}
                                error={buyCurrency === ""}
                                helperText={buyCurrency === "" ? 'Empty field!' : ' '}
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
                        onClick={buttonHandler}>
                        Create Trade
                    </Button>
                </form>
                {/* TODO: FETCH REAL-TIME TRADING DATA */}
            </div>
        </Container>
    );
}

export default withRouter(CreateTrade);