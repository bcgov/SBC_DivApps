# Openshift Templates

## Prerequisites:
* Postgres Database already exists
* Secrets file default name of `camunda` exists with the following secrets:

| Secret name           | Description                                        |
| --------------------- | -------------------------------------------------- |
| db_username           | The username for the main database connection      |
| db_password           | The password for the main database connection      |
| keycloak_realm        | The realm identifier for the OIDC provider         |
| keycloak_clientid     | The username to use for the OIDC connection        |
| keycloak_clientsecret | The secret to use for the OIDC connection          |
| analytics_db_jdbcurl  | The URL for the analytics database connection      |
| analytics_db_username | The username for the analytics database connection |
| analytics_db_password | The password for the analytics database connection |
| formbuilder_bpm_url   | The URL for the formbuilder connection             |
| formbuilder_username  | The username for the formbuilder connection        |
| formbuilder_password  | The password fot the formbuilder connection        |

## Deployment:
Please note that this config creates a configmap that needs to be updated with your own smtp server information.

### Build

Import the buildconfig into your tools workspace

### Deploy

Ensure that the database and secrets file are properly set up, then
```
$ oc process -f camunda_deployconfig.yaml \
    --param-file=camunda_[ENV].param \
    -p APPLICATION_DOMAIN=[YOUR APP DNS NAME] \
    -p KEYCLOAK_URL=https://[YOUR OIDC DNS NAME] \
    -p SMTP_SERVER=[YOUR SMTP DNS NAME] \
    | oc -n hdhclr-[ENV] apply -f -
```
