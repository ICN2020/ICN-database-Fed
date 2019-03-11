# show list of certificates
ndnsec-list -vv

# generate admin key and certificate request
ndnsec-keygen /OGB/cert/admin > admin.req

# autosign admin certificate
ndnsec-certgen -N /OGB/cert/admin -s /OGB/cert/admin admin.req > admin.cert

#install admin certificate
cat admin.cert | ndnsec-cert-install -

# make admin the default certificate
ndnsec-set-default /OGB/cert/admin


#generate keys and certificate request for 'bonvoyage' tenant
ndnsec-keygen /OGB/cert/bonvoyage > bonvoyage.req

# sign tenant certificate with admin key
ndnsec-certgen -N /OGB/cert/bonvoyage -s /OGB/cert/admin bonvoyage.req > bonvoyage.cert

#install bonvoyage cert
cat bonvoyage.cert | ndnsec-cert-install -



# http://www.lists.cs.ucla.edu/pipermail/nfd-dev/2015-March/000905.html
