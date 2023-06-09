#!/usr/bin/env bash
curl localhost:8080/ping

ID=$( curl -d@scenarios/bus_station.scenario -H 'Content-Type: application/json' http://localhost:8080/simulation/start?wait=true )

echo $ID

curl -o thing.zip http://localhost:8080/simulation/results/$ID