# Openshift Templates

## Prerequisites:
* Postgres Database already exists

<br/>
## Orbeon API support:
<br/><br/>
This implementation includes the ability to create an API for basic authentication to submit forms data to BPM.
<br/><br/>
formbuilder_bpm_url format is: 

```
https://{camunda_id}:{camunda_password}@{camunda_url}/camunda
```
The form url to provide to ORBEON Send Action <br/><br/>
Example URL for send action:  

```
https://{camunda_url}/camunda/engine-rest/process-definition/key/CC_Process/start
```

## Deployment:
Please note that this config creates a configmap that needs to be updated with your own smtp server information.

### Build

Import the buildconfig into your tools workspace

### Deploy

Ensure that the database and secrets file are properly set up, then
```
$ oc process -f camunda_deployconfig.yaml --param-file=camunda_dev.param \
    | oc -n [workspace] apply -f -
```

### Additional optional components\

For HA, we are using SESSION_STORE_TYPE of jdbc.  Reference: https://docs.spring.io/spring-boot/docs/current/api/org/springframework/boot/autoconfigure/session/StoreType.html

This requires a table schema in your postgres database:

https://github.com/spring-projects/spring-session/blob/master/spring-session-jdbc/src/main/resources/org/springframework/session/jdbc/schema-postgresql.sql

Other variables that can be added for perfomance adjustments include:

```
HIKARI_MAX_POOLSIZE
HIKARI_CONN_TIMEOUT
JOB_CORE_POOL_SIZE
JOB_LOCK_TIME_MILLIS
JOB_MAXJOBS_PER_ACQUISITION
JOB_MAX_POOL_SIZE
JOB_QUEUE_SIZE
JOB_WAIT_TIME_MILLIS
JOB_MAX_WAIT
```