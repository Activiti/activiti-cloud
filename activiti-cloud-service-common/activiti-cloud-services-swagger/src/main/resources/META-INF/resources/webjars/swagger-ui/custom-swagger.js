function buildFormData(data) {
  let formArr = [];

  for (let name in data) {
    let val = data[name];
    if (val !== undefined && val !== '') {
      formArr.push(
        [name, '=', encodeURIComponent(val).replace(/%20/g, '+')].join(''),
      );
    }
  }
  return formArr.join('&');
}

function getAuth() {
  return window.ui.authSelectors.authorized().toJS()?.oauth;
}

function tryRefreshOauth2Token() {
  const auth = getAuth();
  if (!auth) {
    console.log(`Swagger is not authorized. Can't refresh token.`);
    return;
  }

  const {schema, clientId, token} = auth;
  const errors = [];

  if (schema == null) {
    errors.push('Invalid auth: missing schema');
  }
  if (schema?.tokenUrl == null) {
    errors.push('Invalid auth schema: missing tokenUrl');
  }
  if (clientId == null) {
    errors.push('Invalid auth: missing clientId');
  }

  if (token == null) {
    errors.push('Invalid auth: missing token');
  }
  if (token?.refresh_token == null) {
    errors.push('Invalid auth: missing refresh token');
  }
  if (token?.scope == null) {
    errors.push('Invalid auth: missing scope');
  }
  if (errors.length) {
    console.log("Can't refresh token due to the following issues:");
    errors.forEach(console.log);
    return;
  }

  const form = {
    grant_type: 'refresh_token',
    refresh_token: token.refresh_token,
    client_id: clientId,
    scope: token.scope,
  };

  console.log(`Refreshing token...`);
  window.ui.authActions.authorizeRequest({
    body: buildFormData(form),
    name,
    url: schema.tokenUrl,
    auth,
  });
}

function startClock() {
  function tick() {
    const remainingRefreshTime = window.tokenRefreshTime - Date.now();

    if (remainingRefreshTime < 0) {
      clearInterval(window.tokenClockInterval);
      tryRefreshOauth2Token();
    }
  }

  if (window.tokenClockInterval) clearInterval(window.tokenClockInterval);

  window.tokenClockInterval = setInterval(tick, 500);
  tick();
}

let patchTries = 10;

function patchRefreshHook() {
  if (!window?.ui?.authActions?.authorizeOauth2) {
    if (patchTries) {
      patchTries--;
      setTimeout(patchRefreshHook, 1000);
      console.log(
        'Missing patch target function "window.ui.authActions.authorizeOauth2", retrying in 1s...',
      );
      return;
    }
    console.log(
      'Cannot patch OAuth token refresh hook. Missing patch target function "window.ui.authActions.authorizeOauth2"',
    );
    return;
  }

  console.log('Patching OAuth token refresh hook...');
  const origAuthorizeOauth2 = window.ui.authActions.authorizeOauth2;

  window.ui.authActions.authorizeOauth2 = (payload) => {
    // If the token can expire, schedule a token refresh and update the timer
    if (payload.token.expires_in) {
      const tokenRefreshTimeout = payload.token.expires_in * 750;
      console.log(
        `Refreshable token detected. Scheduling token refresh in ${(
          tokenRefreshTimeout /
          1000 /
          60
        ).toFixed(1)}min (expires in ${(payload.token.expires_in / 60).toFixed(
          1,
        )}min)...`,
      );
      window.tokenRefreshTime = Date.now() + payload.token.expires_in * 750;

      // Start the clock
      startClock();
    }

    return origAuthorizeOauth2(payload);
  };

  startClock();
}

patchRefreshHook();
