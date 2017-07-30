** ALL DEVELOPMENT/DEPLOYMENT IS BASED ON DOCKER **

For development, run "<sudo> docker-compose -p devel -f docker-compose.yaml -f docker-compose-dev.yaml up"

Note that on docker-machine based systems (primarily non-Pro Windows), this directory must be under your
$HOME directory or else it won't make it across the VirtualBox boundary share and the /code volume in 
docker-compose-dev.yaml won't show up.

