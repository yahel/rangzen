require('log-timestamp');

var express = require('express');
var bodyParser = require('body-parser');
var app = express();

// Express declarations
app.use(bodyParser.json());
app.use(bodyParser.urlencoded({
    extended: true
}));

// Server data variables
var phones = {};

// Returns the distance in km between the two given locations.
function CalculateDistance(lat1, lng1, lat2, lng2) {
  var R = 6371; // Radius of Earth in km.
  var dlat = (lat2 - lat1) * Math.PI / 180;
  var dlng = (lng2 - lng1) * Math.PI / 180;
  var a = 0.5 - Math.cos(dlat)/2 + 
          Math.cos(lat1 * Math.PI / 180) *
          Math.cos(lat2 * Math.PI / 180) * 
          (1 - Math.cos(dlng)) / 2;

  return R * 2 * Math.asin(Math.sqrt(a));
};

// Input fields:
//   phoneid : <hex string>
//   friends : [ <friend string>, ... ]
//
// Output fields:
//   status : "ok" or "failed"
function RegisterPhone(req, res) {
  if (!('phoneid' in req.body) ||
      req.body.phoneid in phones) {
    console.log("RegisterPhone failed: " + JSON.stringify(req.body));
    response = { "status" : "failed" };
    res.send(300, JSON.stringify(response));
    return;
  }
  
  console.log("RegisterPhone ok, phone: " + req.body.phoneid);
  phones[req.body.phoneid] = {};
  phones[req.body.phoneid]['friends'] = req.body.friends;
  phones[req.body.phoneid]['locations'] = [];
  phones[req.body.phoneid]['exchanges'] = [];

  response = { "status" : "ok" };
  res.send(200, JSON.stringify(response));
}

// Input fields:
//   phoneid : <hex string>
// 
// Output fields:
//   status : "ok" or "failed"
//   friends : [ <friend string>, ... ]
function GetFriends(req, res) {
  if (!('phoneid' in req.body)) {
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
  if (!('phoneid' in req.body)) {
    console.log("UpdateLocations failed: " + JSON.stringify(req.body));
    response = { "status" : "failed" };
    res.send(300, JSON.stringify(response));
    return;
  }
  
  for (l in req.body.locations) {
    var value = req.body.locations[l];
    if (!('time' in value && 'latitude' in value && 'longitude' in value)) {
      console.log("UpdateLocations skipping malformed value: " + JSON.stringify(value));
      continue;
    }
    phones[req.body.phoneid].locations.push(req.body.locations[l]);
  }

  console.log("UpdateLocations ok, phone: " + req.body.phoneid);
  response = { "status" : "ok" };
  res.send(200, JSON.stringify(response));
}

// Input fields:
//   phoneid : <hex string>
//   peer_phone_id : <hex string>
//   protocol : <string>
//   start_location : { <location tuple> }
//   end_location : { <location tuple> }
//
// Output fields:
//   status : "ok" or "failed"
function UpdateExchange(req, res) {
  if (!('phoneid' in req.body &&
        'peer_phone_id' in req.body &&
        req.body['peer_phone_id'] in phones &&
        'protocol' in req.body &&
        'start_location' in req.body &&
        'end_location' in req.body)) {
    console.log("UpdateExchange failed: " + JSON.stringify(req.body));
    response = { "status" : "failed" };
    res.send(300, JSON.stringify(response));
    return;
  }

  exchange = {}
  exchange['peer_phone_id'] = req.body['peer_phone_id']
  exchange['protocol'] = req.body['protocol']
  exchange['start_location'] = req.body['start_location']
  exchange['end_location'] = req.body['end_location']
  phones[req.body.phoneid].exchanges.push(exchange)

  console.log("UpdateExchange ok, phone: " + req.body.phoneid);
  response = { "status" : "ok" };
  res.send(200, JSON.stringify(response));
}

// Input fields:
//   phoneid : <hex string>
// 
// Output fields:
//   status : "ok" or "failed"
//   locations : [ <location tuple>, ... ]
function GetPreviousLocations(req, res) {
  if (!('phoneid' in req.body)) {
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
  if (!('phoneid' in req.body)) {
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
  if (!('phoneid' in req.body && 'distance' in req.body) ||
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

port = 1337;
app.listen(port);
console.log('Rangzen Location Server listening at http://localhost:' + port)
