function waitUntilInstalled(registration) {
    return new Promise(function(resolve, reject) {
      if (registration.installing) {
      console.log('installing!')
        registration.installing.addEventListener('statechange', function(e) {
          if (e.target.state === 'installed') {
          console.log("installed!")
            resolve();
          } else if (e.target.state === 'redundant') {
          console.log("not installed!")
            reject();
          }
        });
      } else {
        resolve();
      }
    });
  }

if ('serviceWorker' in navigator) {
  navigator.serviceWorker.register('./service-worker.js', { scope: './' })
  .then(waitUntilInstalled)
    .catch(function(error) {
      console.error(error);
    });
}