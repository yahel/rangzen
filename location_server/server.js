require('log-timestamp')(function() { return '[' + new Date().toString() + '] %s' });


var express = require('express');
var bodyParser = require('body-parser');

var arango = require('arangojs');
var async = require('async');
var uP = require('micropromise');

var app = express();
var db = null;

// Express declarations
app.use(bodyParser.json());
app.use(bodyParser.urlencoded({
    extended: true
}));

// Server data variables.
// Each phone contains:
// - 'friends' : [ friend list ]
// - 'locations' : [ { location data }, { location data }, ... ]
// - 'exchanges' : [ { exchange data }, { exchange data }, ... ]
var phones = {};

// Returns the distance in km between the two given locations.
function CalculateDistance(lat1, lng1, lat2, lng2) {
  var R = 6371; // Radius of Earth in km.
  var dlat = (lat2 - lat1) * Math.PI / 180;
  var dlng = (lng2 - lng1) * Math.PI / 180;
  var a = 0.5 - Math.cos(dlat) / 2 + 
          Math.cos(lat1 * Math.PI / 180) *
          Math.cos(lat2 * Math.PI / 180) * 
          (1 - Math.cos(dlng)) / 2;

  return R * 2 * Math.asin(Math.sqrt(a));
};

// Populates the local phone var with the given phone info.  Used upon startup
// or when a phone register call comes in.
function InitPhone(phone) {
  phones[phone.phoneid] = {};
  phones[phone.phoneid]['friends'] = phone.friends;
  phones[phone.phoneid]['locations'] = [];
  phones[phone.phoneid]['exchanges'] = [];
}

// Input fields:
//   phoneid : <hex string>
//   friends : [ <friend string>, ... ]
//
// Output fields:
//   status : "ok" or "failed"
function RegisterPhone(req, res) {
  if (!('phoneid' in req.body)) {
    console.log("RegisterPhone failed: " + JSON.stringify(req.body));
    response = { "status" : "failed" };
    res.send(300, JSON.stringify(response));
    return;
  } else if (req.body.phoneid in phones) {
    response = { "status" : "ok" };
    res.send(200, JSON.stringify(response));
    return;
  }
  
  // Add this phone's registration to the database.
  db.document.create('phones', { 'phoneid' : req.body.phoneid,
                                 'friends' : req.body.friends }).then(function(r) {
    InitPhone(req.body);
    console.log("RegisterPhone ok: " + JSON.stringify(req.body));
    response = { "status" : "ok" };
    res.send(200, JSON.stringify(response));
  }, function(err) {
    console.log("Couldn't store phone registration to database: %j", err);
    response = { "status" : "failed" };
    res.send(300, JSON.stringify(response));
  });
}

// Input fields:
//   phoneid : <hex string>
// 
// Output fields:
//   status : "ok" or "failed"
//   friends : [ <friend string>, ... ]
function GetFriends(req, res) {
  if (!('phoneid' in req.body) ||
      !(req.body.phoneid in phones)) {
    console.log("GetFriends failed: " + JSON.stringify(req.body));
    response = { "status" : "failed" };
    res.send(300, JSON.stringify(response));
    return;
  }

  console.log("GetFriends ok, phone: " + req.body.phoneid);

  response = { "status" : "ok",
               "friends" : phones[req.body.phoneid].friends };
  res.send(200, JSON.stringify(response));
}

// Input fields:
//   phoneid : <hex string>
//   locations : [ { time : <time>, latitude : <latitude>, longitude : <longitude> },
//                 { time : <later_time>, latitude : <latitude>, longitude : <longitude> } ]
//
// Output fields:
//   status : "ok" or "failed"
function UpdateLocations(req, res) {
  if (!('phoneid' in req.body) ||
      !(req.body.phoneid in phones)) {
    console.log("UpdateLocations failed: " + JSON.stringify(req.body));
    response = { "status" : "failed" };
    res.send(300, JSON.stringify(response));
    return;
  }
 
  async.eachSeries(req.body.locations, function(value, callback) {
    if (!('time' in value && 'latitude' in value && 'longitude' in value)) {
      console.log("UpdateLocations skipping malformed value: " + JSON.stringify(value));
      callback();
      return;
    }

    // Add this location update to the database and save it locally.
    newdoc = value;
    newdoc.phoneid = req.body.phoneid;
    db.document.create('locations', newdoc).then(function(r) {
      phones[req.body.phoneid].locations.push(value);
      callback();
    }, function(err) {
      console.log("Couldn't store phone location to database: %j", err);
      callback();
    });
  }, function(err) {
    if (err) {
      console.log("UpdateLocations failed: " + JSON.stringify(req.body));
      response = { "status" : "failed" };
      res.send(300, JSON.stringify(response));
    } else {
      console.log("UpdateLocations ok, phone: " + req.body.phoneid);
      response = { "status" : "ok" };
      res.send(200, JSON.stringify(response));
    }
  });
}

// Input fields:
//   phoneid : <hex string>
//   peer_phone_id : <hex string>
//   protocol : <string>
//   start_time : <time>
//   end_time : <time>
//   start_location : { <location tuple> }
//   end_location : { <location tuple> }
//
// Output fields:
//   status : "ok" or "failed"
function UpdateExchange(req, res) {
  if (!('phoneid' in req.body &&
        req.body.phoneid in phones &&
        'peer_phone_id' in req.body &&
        req.body['peer_phone_id'] in phones &&
        'protocol' in req.body &&
        'start_time' in req.body &&
        'end_time' in req.body &&
        'start_location' in req.body &&
        'end_location' in req.body)) {
    console.log("UpdateExchange failed: " + JSON.stringify(req.body));
    response = { "status" : "failed" };
    res.send(300, JSON.stringify(response));
    return;
  }

  exchange = {}
  exchange['peer_phone_id'] = req.body['peer_phone_id'];
  exchange['protocol'] = req.body['protocol'];
  exchange['start_time'] = req.body['start_time'];
  exchange['end_time'] = req.body['end_time'];
  exchange['start_location'] = req.body['start_location'];
  exchange['end_location'] = req.body['end_location'];

  // Add this exchange to the database.
  newdoc = exchange;
  newdoc.phoneid = req.body.phoneid;
  db.document.create('exchanges', newdoc).then(function(r) {
    console.log("UpdateExchange ok, phone: " + req.body.phoneid);
    phones[req.body.phoneid].exchanges.push(exchange);

    response = { "status" : "ok" };
    res.send(200, JSON.stringify(response));
  }, function(err) {
    console.log("Couldn't store exchange to database: %j", err);
    response = { "status" : "failed" };
    res.send(300, JSON.stringify(response));
  });
}

// Input fields:
//   phoneid : <hex string>
// 
// Output fields:
//   status : "ok" or "failed"
//   locations : [ <location tuple>, ... ]
function GetPreviousLocations(req, res) {
  if (!('phoneid' in req.body) ||
      !(req.body.phoneid in phones)) {
    console.log("GetPreviousLocations failed: " + JSON.stringify(req.body));
    response = { "status" : "failed" };
    res.send(300, JSON.stringify(response));
    return;
  }

  console.log("GetPreviousLocations ok, phone: " + req.body.phoneid);

  response = { "status" : "ok",
               "locations" : phones[req.body.phoneid].locations };
  res.send(200, JSON.stringify(response));
}

// Input fields:
//   phoneid : <hex string>
// 
// Output fields:
//   status : "ok" or "failed"
//   exchanges : [ <exchange tuple>, ... ]
function GetPreviousExchanges(req, res) {
  if (!('phoneid' in req.body) ||
      !(req.body.phoneid in phones)) {
    console.log("GetPreviousExchanges failed: " + JSON.stringify(req.body));
    response = { "status" : "failed" };
    res.send(300, JSON.stringify(response));
    return;
  }

  console.log("GetPreviousExchanges ok, phone: " + req.body.phoneid);

  response = { "status" : "ok",
               "exchanges" : phones[req.body.phoneid].exchanges };
  res.send(200, JSON.stringify(response));
}

// Input fields:
//   phoneid : <hex string>
//   distance : <float in km>
// 
// Output fields:
//   status : "ok" or "failed"
//   phones : [ <hex strings>, ... ]
function GetNearbyPhones(req, res) {
  if (!('phoneid' in req.body &&
        req.body.phoneid in phones &&
        'distance' in req.body) ||
      isNaN(parseFloat(req.body.distance))) {
    console.log("GetNearbyPhones failed: " + JSON.stringify(req.body));
    response = { "status" : "failed" };
    res.send(300, JSON.stringify(response));
    return;
  }

  var dist_limit = parseFloat(req.body.distance);

  if (phones[req.body.phoneid].locations.length < 1) {
    console.log("GetNearbyPhones failed, current phone has no locations: " + JSON.stringify(req.body));
    response = { "status" : "failed" };
    res.send(300, JSON.stringify(response));
    return;
  }
  var us_loc = phones[req.body.phoneid].locations[phones[req.body.phoneid].locations.length - 1];

  var local_phones = [];
  for (p in phones) {
    if (p == req.body.phoneid) continue;
    if (phones[p].locations.length < 1) continue;

    var them_loc = phones[p].locations[phones[p].locations.length - 1];
    var dist = CalculateDistance(us_loc.latitude, us_loc.longitude, them_loc.latitude, them_loc.longitude);
    if (dist <= dist_limit) {
      local_phones.push(p);
    }
  }
  
  console.log("GetNearbyPhones ok, phone: " + req.body.phoneid);
  response = { "status" : "ok",
               "phones" : local_phones };
  res.send(200, JSON.stringify(response));
}

// Input fields:
//   <none>
// 
// Output fields:
//   status : "ok" 
function Reset(req, res) {
  console.log("Reset server, clearing out all data.");
  phones = [];
  response = { "status" : "ok" };
  res.send(200, JSON.stringify(response));
}

// Ensures that the backing database exists and that the appropriate collections
// (tables) are present.
function SetUpDatabase() {
  db = arango.Connection("http://localhost:8529/");

  // Ensure that the collections we need exist.
  collection_names = [ 'phones', 'locations', 'exchanges' ];
  for (c in collection_names) {
    db.collection.create(collection_names[c]).done(function(res) {
      console.log("Creating collection.  Result: %j", res);
    }, function(err) {
      var arangodb_duplicate_error_code = 1207;
      if (err.errorNum != arangodb_duplicate_error_code) {
        console.log("Can't proceed without collection. Exiting. Error: %j", err);
        process.exit(1);
      } else {
        console.log("Collection already exists.  Ok.");
      }
    });
  }
}

// Loads previously-registered phones into soft state.
function LoadRegisteredPhones() {
  var docdump = uP();

  db.query.string = "FOR d IN @@collection RETURN d._id";
  db.query.exec({'@collection' : 'phones'}, function(err, doclist) {
    if (err) {
      console.log("Can't load existing phone data. Exiting. Error: %j", err);
      process.exit(1);
    }

    async.map(doclist.result, function(d, callback) {
      // Create a promise for the db reply.
      var pendingReply = uP();

      // For each reply we get, fulfill the pending reply var.
      db.document.get(d).then(function(doc) {
        pendingReply.fulfill(doc);
      },
      function(err) {
        console.log("error %j", err);
        pendingReply.reject(err);
      });

      // Block for the pending database reply.
      pendingReply.then(function(v) {
        callback(null, v);
      },
      function(err) {
        console.log("error:", err);
        callback(err, null);
      });
    }, function(err, results) {
      if (err) {
        console.log("Map failed while loading existing phone data: %j. Exiting.", map);
        process.exit(1);
      } else {
        docdump.fulfill(results);
      }
    });
  });

  // Wait until the async calls are finished and have populated the dump of
  // existing documents.
  docdump.then(function(v) {
    console.log("Loaded existing phone data: %j", v);
    for (p in v) {
      InitPhone(v[p]);
    }
  }, function(err) {
    console.log("Can't load existing phone data. Exiting. Error: %j", err);
    process.exit(1);
  });
}

// Starts listening for RPCs.
function RunServer() {
  port = 1337;
  app.listen(port);
  console.log('Rangzen Location Server listening at http://localhost:' + port)
}

// API for Rangzen Location Server
app.post('/register_phone', RegisterPhone);
app.post('/get_friends', GetFriends);
app.post('/update_locations', UpdateLocations);
app.post('/update_exchange', UpdateExchange);
app.post('/get_previous_locations', GetPreviousLocations);
app.post('/get_previous_exchanges', GetPreviousExchanges);
app.post('/get_nearby_phones', GetNearbyPhones);

// TODO(lerner): Disable Reset in the live deployment.
app.post('/reset', Reset);

// Before starting up, ensure the db is setup, and re-read phone registrations
// from the db.
SetUpDatabase();
process.nextTick(LoadRegisteredPhones);

RunServer();
