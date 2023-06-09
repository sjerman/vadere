#!/usr/bin/env bash
curl localhost:8080/ping

ID=$( curl -d@scenarios/bus_station.scenario -H 'Content-Type: application/json' http://localhost:8080/simulation/start )

echo $ID

curl -o res1.zip http://localhost:8080/simulation/results/$ID

ID=$( curl -d@scenarios/bus_station.scenario -H 'Content-Type: application/json' http://localhost:8080/simulation/start )

sleep 2
curl -o s1.json http://localhost:8080/simulation/status/$ID
sleep 2
curl -o s2.json http://localhost:8080/simulation/status/$ID 

sleep 2
curl -o s3.json http://localhost:8080/simulation/status/$ID 

sleep 10
curl -o s4.json http://localhost:8080/simulation/status/$ID 
curl -o res2.zip http://localhost:8080/simulation/results/$ID
