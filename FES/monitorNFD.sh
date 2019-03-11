while true; do 
    if [ ! -e /var/run/nfd.sock ]; then 
    	echo "NFD restarted"
        nfd-start &
        sleep 1
        nfdc register /repo/repo1 tcp://192.168.68.3
        ssh ogb@192.168.68.3 "nfdc register /FES/ tcp://192.168.68.101"
    fi
done
