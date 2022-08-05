import Grid from "@material-ui/core/Grid";
import TextField from "@material-ui/core/TextField";
import {InputAdornment} from "@material-ui/core";
import Button from "@mui/material/Button";
import React from "react";

const IssueMoney = (props) => (

    <form className={props.forms} style={{marginLeft: "20px"}} id="issueMoneyForm" noValidate>
        <Grid item xs={10}>
            <TextField
                required size="small" name="moneyAmountToIssue" variant="outlined" type="number"
                fullWidth id="moneyAmountToIssue" label="Amount" placeholder=""
                InputProps={{
                    startAdornment: (
                        <InputAdornment position="start">$</InputAdornment>)
                }}
                onChange={props.moneyAmountToIssueChange}
                error={props.moneyAmountToIssue === "" || props.moneyAmountToIssue <= 0}
                helperText={props.moneyAmountToIssue === "" ? 'Empty field!' : ' '}/>
        </Grid>
        <Grid item xs={10}>
            <Button size="small" fullWidth type="submit" variant="contained" color="primary" onClick={props.issueMoney}>Issue
                Money</Button>
        </Grid>
    </form>

);

export default IssueMoney;