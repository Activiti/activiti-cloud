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

  const {schema, clientId} = auth;
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
  if (errors.length) {
    console.log("Can't refresh token due to the following issues:");
    errors.forEach(console.log);
    return;
  }

  let refreshToken = null;
  let refreshTokenKey = null;
  let scope = null;
  let scopeKey = null;
  const result = findTokenAndScope();
  refreshToken = result.refreshTokenValue;
  refreshTokenKey = result.refreshTokenKey;

  scope = result.scopeValue;
  scopeKey = result.scopeKey;

  if (refreshToken && scope) {
    console.log('Refresh token:', refreshToken);
    console.log('Scope:', scope);
  } else {
    console.log('Refresh token or scope not found.');
  }

  const form = {
    grant_type: 'refresh_token',
    refresh_token: refreshToken,
    client_id: clientId,
    scope: scope,
    audience: undefined,
  };

  console.log(`Refreshing token...`);

  fetch(schema.tokenUrl, {
    method: 'POST',
    headers: {
      'accept': 'application/json, text/plain, */*',
      'content-type': 'application/x-www-form-urlencoded',
    },
    body: buildFormData(form)
  })
    .then(response => response.json())
    .then(data => {

      if (data) {
        const refreshToken = data.refresh_token;
        const scope = data.scope;

        if (refreshToken) {
          localStorage.setItem(refreshTokenKey, refreshToken);
          console.log('Refresh token:', refreshToken);
        }

        if (scope) {
          localStorage.setItem(scopeKey, scope);
          console.log('Scope:', scope);
        }
      } else {
        console.log('No data in response:', data);
      }

      console.log('Access token refreshed:', data.refresh_token);

      const token = data;

      const ui = window.ui;
      if (ui?.authActions?.authorizeOauth2WithPersistOption && token) {
        ui.authActions.authorizeOauth2WithPersistOption({auth, token})
      } else {
        console.error('Swagger UI authActions not available.');
      }
    })
    .catch(error => console.error('Error refreshing token:', error));
}

function findTokenAndScope() {
  let refreshTokenKey = null;
  let refreshTokenValue = null;
  let scopeKey = null;
  let scopeValue = null;


  for (let i = 0; i < localStorage.length; i++) {
    const key = localStorage.key(i);

    if (key?.includes('refresh_token')) {
      refreshTokenKey = key;
      refreshTokenValue = localStorage.getItem(key);
    } else if (key?.includes('granted_scopes')) {
      scopeKey = key;
      scopeValue = localStorage.getItem(key);
    }

    // Break the loop early if both values are found
    if (scopeValue && refreshTokenValue) {
      break;
    }
  }

  return {
    refreshTokenValue: refreshTokenValue,
    refreshTokenKey: refreshTokenKey,
    scopeValue: scopeValue,
    scopeKey: scopeKey
  };
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
      const tokenExpiryTimeout = payload.token.expires_in;
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
      window.tokenExpiryTime = Date.now() + payload.token.expires_in * 1000;

      // Start the clock
      startClock();
    }

    return origAuthorizeOauth2(payload);
  };

  startClock();
}

patchRefreshHook();
