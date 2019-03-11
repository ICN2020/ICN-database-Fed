#!/bin/bash
#nfd-stop
#sleep 1
nfd-start
sleep 1
nfdc register /repo/repo1 tcp://192.168.78.210
nfdc register /repo/repo2 tcp://192.168.78.211
nfdc register /repo/repo3 tcp://192.168.78.212
ssh ogb@192.168.78.210 "nfdc register /FES/ tcp://192.168.78.110"
ssh ogb@192.168.78.211 "nfdc register /FES/ tcp://192.168.78.110"
ssh ogb@192.168.78.212 "nfdc register /FES/ tcp://192.168.78.110"
screen -S screenSession -t win0 -A -d -m
screen -S screenSession -X screen -t win1
screen -S screenSession -p win0 -X stuff $'cd /home/ogb/ogb3/FES\njava -Xmx2048M -jar target/FES-3.0.jar\n'
sleep 1
screen -S screenSession -p win1 -X stuff $'cd /home/ogb/ogb3/FES\nsh monitorNFD_root.sh\n'
sleep 2
echo WARNING: be shure FES is running, may be necessary to enter sudoer password in detached screen. Run screen -r
