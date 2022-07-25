/* Timezone settings */
const timezone = '(UTC)';
const location = 'Etc/UTC';

const URL = 'http://localhost:';
const headers = {'Access-Control-Allow-Origin': '*', 'Content-Type': 'application/json'};

/* names with spaces (from clients build.gradle: '--server.port=XXXXX') */
const parties = {"O=PartyA, L=London, C=GB": 10056, "O=PartyB, L=New York, C=US": 10057}

export { timezone, location, URL, headers, parties };