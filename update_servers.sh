#!/bin/bash

MACHINE=open-12.cs.mcgill.ca

# User dependent - Please Change before running the script
USER=akaba2
PASS=`cat cs-password.pass`
PROJECT_PATH='COMP512---A1/'

echo "cd ${PROJECT_PATH} && git pull origin master" | sshpass -p ${PASS} ssh -o StrictHostKeyChecking=no ${USER}@${MACHINE}