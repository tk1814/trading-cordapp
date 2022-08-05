import React from 'react';
import Grid from "@material-ui/core/Grid";
import {FormControl, InputLabel, MenuItem, Select} from "@material-ui/core";
import TextField from "@material-ui/core/TextField";
import Button from "@mui/material/Button";

const IssueStocks = (props) => (

    <form className={props.forms} style={{marginLeft: "20px", marginTop: "10px"}} id="issueStockForm" noValidate>
        <Grid container spacing={3}>
            <Grid item xs={10}>
                <FormControl required fullWidth>
                    <InputLabel id="demo-simple-select-label">Stock</InputLabel>
                    <Select
                        // value={}
                        defaultValue={''} labelId="demo-simple-select-label"
                        id="demo-simple-select" label="Stock Name"
                        onChange={props.stockNameToIssueChange}>
                        {props.stockCodes.map((stock, key) => (<MenuItem key={key} value={stock}>{stock}</MenuItem>))}
                    </Select>
                </FormControl>
            </Grid>
            <Grid item xs={10}>
                <TextField
                    size="small" required name="stockAmountToIssue" variant="outlined"
                    fullWidth id="stockAmountToIssue" label="Volume" type="number"
                    onChange={props.stockAmountToIssueChange}
                    error={props.stockAmountToIssue === "" || props.stockAmountToIssue <= 0}
                    helperText={props.stockAmountToIssue === "" ? 'Empty field!' : ' '}
                />
            </Grid>
            <Grid item xs={10}>
                <Button style={{marginTop: "-40px"}} size="small" fullWidth type="submit"
                        variant="contained" color="primary" onClick={props.issueStock}>Issue Stocks </Button>
            </Grid>
        </Grid>
    </form>
);

export default IssueStocks;