#!/bin/bash

#           Flights Server      Cars Server             Rooms Server       Customers server      MiddleWare
MACHINES=(lab2-24.cs.mcgill.ca lab2-25.cs.mcgill.ca lab2-26.cs.mcgill.ca lab2-27.cs.mcgill.ca lab2-28.cs.mcgill.ca)

# User dependent -
USER=akaba2
PASS=`cat cs-password.pass`
SERVER_PATH='COMP512---A1/Server'

## TODO(Abdullah): can you take a look?

tmux new-session \; \
	split-window -h \; \
	split-window -v \; \
	split-window -v \; \
	split-window -v \; \
	select-layout main-vertical \; \
	select-pane -t 1 \; \
	send-keys "sshpass -p ${PASS} ssh -o StrictHostKeyChecking=no ${USER}@${MACHINES[0]} \"cd ${SERVER_PATH} > /dev/null; git pull origin master; make; echo -n 'Connected to '; hostname; ./run_server.sh Flights\"" C-m \; \
	select-pane -t 2 \; \
	send-keys "sshpass -p ${PASS} ssh -o StrictHostKeyChecking=no ${USER}@${MACHINES[1]} \"cd ${SERVER_PATH} > /dev/null; git pull origin master; make; echo -n 'Connected to '; hostname; ./run_server.sh Cars\"" C-m \; \
	select-pane -t 3 \; \
	send-keys "sshpass -p ${PASS} ssh -o StrictHostKeyChecking=no ${USER}@${MACHINES[2]} \"cd ${SERVER_PATH} > /dev/null; git pull origin master; make; echo -n 'Connected to '; hostname; ./run_server.sh Rooms\"" C-m \; \
	select-pane -t 4 \; \
	send-keys "sshpass -p ${PASS} ssh -o StrictHostKeyChecking=no ${USER}@${MACHINES[3]} \"cd ${SERVER_PATH} > /dev/null; git pull origin master; make; echo -n 'Connected to '; hostname; ./run_server.sh Customers\"" C-m \; \
	select-pane -t 0 \; \
	send-keys "sshpass -p ${PASS} ssh -o StrictHostKeyChecking=no ${USER}@${MACHINES[4]} \"cd ${SERVER_PATH} > /dev/null; git pull origin master; make; echo -n 'Connected to '; hostname; sleep .5s; ./run_middleware.sh ${MACHINES[0]} ${MACHINES[1]} ${MACHINES[2]} ${MACHINES[3]}\"" C-m \;