#!/usr/bin/env bash

echo "Deleting files"
./clean_files.sh

# Kill all processes listeing to the needed ports
PORTS=(54000 54001 54002 54003 54004 54005 54006)
for i in 0 1 2 3 4
do
    pid=$(lsof -i:${PORTS[$i]} -t)

    if [[ $pid ]]; then
        echo 'killing ' $pid
        kill -9  $pid > /dev/null 2>&1
        sleep .2
    fi

done