This file spins up a Apicurio Registry with Postgres as persistent storage and Keycloak as authenticator.

To spin up all the containers

```
docker-compose up -d
```

Next Set up Keycloak:

1. Verify Keycloak Client Configuration
Access Keycloak Admin Console at http://localhost:8081 (admin/admin) and:

2. Ensure the realm "apicurio" exists or create it

3. Check the client "apicurio" or create it

    - Client ID: apicurio

    - Root URL: http://localhost:8080

    - Valid Redirect URIs: http://localhost:8080/*

    - Web Origins: * (or http://localhost:8080)

    - Client Authentication: ON

    - Credentials > Secret: Must match QUARKUS_OIDC_CREDENTIALS_SECRET (currently xxxx)



Note:

- Once you create the client, update docker-compose file with the client-secret, `QUARKUS_OIDC_CREDENTIALS_SECRET`