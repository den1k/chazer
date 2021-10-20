var CACHE_VERSION = 1;
var CURRENT_CACHES = {
  prefetch: 'prefetch-cache-v' + CACHE_VERSION
};

var urlsToPrefetch = [
  "/img/chazer-spins.mp4",
  "/img/chazer-infomercial.mp4"
];

self.addEventListener('install', function(event) {
  self.skipWaiting();

  event.waitUntil(
    caches.open(CURRENT_CACHES.prefetch).then(function(cache) {
    console.log("DOING")
      return cache.addAll(urlsToPrefetch);
    })
  );
});

self.addEventListener('activate', function(event) {
  //delete all caches that aren't named in CURRENT_CACHES
  var expectedCacheNames = Object.keys(CURRENT_CACHES).map(function(key) {
    return CURRENT_CACHES[key];
  });

  event.waitUntil(
    caches.keys().then(function(cacheNames) {
      return Promise.all(
        cacheNames.map(function(cacheName) {
          if (expectedCacheNames.indexOf(cacheName) === -1) {
            return caches.delete(cacheName);
          }
        })
        );
    })
    );
});

self.addEventListener('fetch', function(event) {
  console.log('Handling fetch event for', event.request.url);

  if (event.request.headers.get('range')) {
    var pos =
    Number(/^bytes\=(\d+)\-$/g.exec(event.request.headers.get('range'))[1]);
    console.log('Range request for', event.request.url,
      ', starting position:', pos);
    event.respondWith(
      caches.open(CURRENT_CACHES.prefetch)
      .then(function(cache) {
        var internalUrl = ""
        urlsToPrefetch.forEach(function (item) {
          if(event.request.url.indexOf(item) > 0){
            internalUrl = item;
          }
        });
        return cache.match(internalUrl);
      }).then(function(res) {
        if (!res) {
          return fetch(event.request)
          .then(res => {
            return res.arrayBuffer();
          });
        }
        return res.arrayBuffer();
      }).then(function(ab) {
        return new Response(
          ab.slice(pos),
          {
            status: 206,
            statusText: 'Partial Content',
            headers: [
              // ['Content-Type', 'video/webm'],
              ['Content-Range', 'bytes ' + pos + '-' +
                (ab.byteLength - 1) + '/' + ab.byteLength]]
          });
      }));
  } else {
    console.log('Non-range request for', event.request.url);
    event.respondWith(
    caches.match(event.request).then(function(response) {
      if (response) {
        console.log('Found response in cache:', response);
        return response;
      }
      console.log('No response found in cache. About to fetch from network...');
      return fetch(event.request).then(function(response) {
        console.log('Response from network is:', response);

        return response;
      }).catch(function(error) {
        console.error('Fetching failed:', error);
        throw error;
      });
    })
    );
  }
});