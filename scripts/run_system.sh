#!/usr/bin/env bash
cd ..
mvn package
cd scripts/

function ctrl_c() {
        echo "Deleting files"
        ./clean_files.sh
        exit 0
}

# trap ctrl-c and call ctrl_c()
trap 'ctrl_c' SIGINT

# Flights, Rooms, Cars, MiddleWare
MACHINES=(open-12.cs.mcgill.ca
          open-13.cs.mcgill.ca
          open-14.cs.mcgill.ca
          open-16.cs.mcgill.ca)

# Kill all processes listeing to the needed ports
PORTS=(54002 54003 54004 54006)

# User dependent - Please Change before running the script
USER=akaba2
PASS=`cat cs-password.pass`
PROJ_PATH='COMP512---A1'

# localhost behaviour
if [ $1 = "local" ]; then
    echo 'Running all servers locally'
    ./run_server.sh ${PORTS[0]} Flights &
    ./run_server.sh ${PORTS[1]} Rooms &
    ./run_server.sh ${PORTS[2]} Cars &
    sleep 1
    ./run_middleware.sh localhost localhost localhost ${PORTS[0]} ${PORTS[1]} ${PORTS[2]} &
    echo 'Done!'

    # only exit with a ctrl-c
    while :
    do
	    sleep 0.1
    done

fi

if [ $1 = "local-split" ]; then
    tmux new-session \; \
        split-window -h \; \
        split-window -v \; \
        split-window -v \; \
        select-layout main-vertical \; \
        select-pane -t 1 \; \
        send-keys "./run_server.sh ${PORTS[0]} Flights" C-m \; \
        select-pane -t 2 \; \
        send-keys "./run_server.sh ${PORTS[1]} Rooms" C-m \; \
        select-pane -t 3 \; \
        send-keys "./run_server.sh ${PORTS[2]} Cars" C-m \; \
        select-pane -t 0 \; \
        send-keys "sleep 1 && ./run_middleware.sh localhost localhost localhost ${PORTS[0]} ${PORTS[1]} ${PORTS[2]} " C-m \;
fi

if [ $1 = "remote" ]; then
    tmux new-session \; \
    	split-window -h \; \
    	split-window -v \; \
    	split-window -v \; \
    	select-layout main-vertical \; \
    	select-pane -t 1 \; \
    	send-keys "sshpass -p ${PASS} ssh -o StrictHostKeyChecking=no ${USER}@${MACHINES[0]} \"cd ${PROJ_PATH} > /dev/null; mvn package; echo -n 'Connected to '; hostname; cd scripts > /dev/null; ./run_server.sh ${PORTS[0]} Flights\"" C-m \; \
    	select-pane -t 2 \; \
    	send-keys "sshpass -p ${PASS} ssh -o StrictHostKeyChecking=no ${USER}@${MACHINES[1]} \"cd ${PROJ_PATH} > /dev/null; mvn package; echo -n 'Connected to '; hostname; cd scripts > /dev/null; ./run_server.sh ${PORTS[1]} Rooms\"" C-m \; \
    	select-pane -t 3 \; \
    	send-keys "sshpass -p ${PASS} ssh -o StrictHostKeyChecking=no ${USER}@${MACHINES[2]} \"cd ${PROJ_PATH} > /dev/null; mvn package; echo -n 'Connected to '; hostname; cd scripts > /dev/null; ./run_server.sh ${PORTS[2]} Cars\"" C-m \; \
    	select-pane -t 0 \; \
    	send-keys "sshpass -p ${PASS} ssh -o StrictHostKeyChecking=no ${USER}@${MACHINES[3]} \"cd ${PROJ_PATH} > /dev/null; mvn package; echo -n 'Connected to '; hostname; cd scripts > /dev/null; sleep 1.5s; ./run_middleware.sh ${MACHINES[0]} ${MACHINES[1]} ${MACHINES[2]} ${PORTS[0]} ${PORTS[1]} ${PORTS[2]} \"" C-m \;
fi