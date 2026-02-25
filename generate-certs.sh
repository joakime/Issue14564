#!/bin/bash
set -e

echo "Generating self-signed certificates for testing..."

rm -rf tls
mkdir -p tls
cd tls

# Generate CA
openssl genrsa -out ca.key 2048
openssl req -new -x509 -days 365 -key ca.key -out ca.crt -subj "/CN=Test CA"

# Generate server certificate
openssl genrsa -out tls.key 2048
openssl req -new -key tls.key -out tls.csr -subj "/CN=localhost"
openssl x509 -req -days 365 -in tls.csr -CA ca.crt -CAkey ca.key -CAcreateserial -out tls.crt

# Create JKS keystores
keytool -import -trustcacerts -alias ca -file ca.crt -keystore truststore.jks -storepass changeit -noprompt

openssl pkcs12 -export -in tls.crt -inkey tls.key -out tls.p12 -password pass:changeit
keytool -importkeystore -srckeystore tls.p12 -srcstoretype PKCS12 -srcstorepass changeit \
  -destkeystore keystore.jks -deststorepass changeit -noprompt

echo "Certificates generated in tls/ directory"
