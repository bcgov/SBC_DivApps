# Nifi Documentation

Outline:

The nifi implementation reads data from camunda postgres database and writes them to the analytics database.

## Table of Content
1. [Setup](#setup)
   * [Nifi Configuration](#Nifi-Configuration)
        * [Controller Services](#Controller-Services)
        * [Parameter Context and Parameters](#Parameter-Context)
        * [Controller Services](#Controller-Services)
   * [Camunda POSTGRES setup](#Camunda-POSTGRES-setup)
        * [Step 1: Create analytics tables:](#Create-analytics-tables)
2. [Nifi Flow Description](#Nifi-Flow-Description)  
3. [Failure Notifications](#Failure-Notifications)
4. [How to debug?](#How-to-debug?)
5. [Nifi Registry](#Nifi-Registry)
6. [Logging](#Logging)

## Setup

### Nifi Configuration

#### Controller Services
![Controller Services](images/Controller%20Services.png)

#### Parameter Context
![Parameter Context](images/Parameter%20Context.png)

![Parameters](images/Parameters.png)

Following are the parameters:

#### Camunda Database:

- PG_DRIVER_LOCATION: /opt/nifi/data/extensions/
- PG_BC_HOST_PORT: formsflowai-rw.e69aae-test.svc:5432
- PG_BC_DATABASE: `<camunda database>`
- PG_BC_USERNAME: `<Postgres username>`

#### Analytics Database:
- MSSQL_DRIVER_LOCATION: /opt/nifi/data/extensions/mssql-jdbc-10.2.0.jre8.jar
- MSSQL_USERNAME: `<Analytics Username>`
- MSSQL_DATABASE: `<Analytics database name>`
- MSSQL_HOST_PORT: `<IP ADDRESS and PORT>`
- MSSQL_DRIVER_CLASS_NAME: com.microsoft.sqlserver.jdbc.SQLServerDriver

#### Email notifications:
- SMTP_USERNAME: dotnotreply@gov.bc.ca
- SMTP_HOST: `<value>`
- SMTP_PORT: 25
- SMTP_NOTIFICATION_ENABLED: true
- SMTP_NOTIFICATION_EMAIL: `<comma separated emails>`
- SMTP_FROM_EMAIL: donotreply@gov.bc.ca

#### Teams notifications:
- MICROSOFT_TEAMS_NOTIFICATION_ENABLED: true
- MST_NOTIFICATION_WEBHOOK: `<Teams Webhook>`

#### Others:
- CACHE_LOCATION: ./data/cache
- ERROR_NOTIFICATION_TITLE: Nifi: (test) Message from pipeline

#### Passwords:
The database passwords must be added to the database controllers. They are not set up in the parameters to avoid exposure.

### Camunda POSTGRES setup

#### Step 1: Create analytics tables:
Every analytics table in use must have a corresponding table in the camunda database. This can be done by generating the DDL from analytics and setting up the necessary tables in postgres.

#### Step 2: Add column `nifi_entry_id`
Every table must have a new column called nifi_entry_id. The datatype in postgres for this column is `serial`. Use the following query to alter the table structure:

```
alter table {tablename}
add column nifi_entry_id serial;
```

*What is the purpose of this `nifi_entry_id` column?*

This is an autoincrementing column which is used by the `QueryDatabaseRecord` processor to keep track of which row was last read so that the same row does not keep getting read repeatedly.


### Nifi Flow Description

The fundamental steps for this nifi flow involves just three stages:
- Read data from camunda table (identified uniquely by the pid (process id))
    - This stage includes datatype conversions required.
- Check if the pid already exists in the analytics table
    - `INSERT`, If the pid does not exist
    - `UPDATE`, If the pid exists
- Write the data to the analytics table
- Delete from the camunda table the row corresponding to pid

### Failure Notifications

### How to debug?

To simplify debugging, the failures are classified into two:
- Data has not been written to analytics
- Data has been written to analytics

The error notification email and Teams post contain both `pid` and `table` fields which can be used as input.

#### *Case 1: Database has not been written to Analytics:*

Use the `QueryDatabaseTable` processor with the title `(Debug Workflow) Query Any Table - Edit before execution`

![Debug Workflow - Query any table](images/Debug%20Workflow%20-%20Query%20Any%20Table.png)

Input required:
- `pid` - Enter this value in the `Additional WHERE clause`.
- `table name`: Enter this value in `Table Name` property.

Refer the following image to locate the `pid` and `table name` fields.
![Debug Workflow - Query any table - Properties](images/Debug%20Workflow%20-%20Query%20Any%20Table%20-%20Properties.png)

Debugging Steps:

    1. Update the Table Name field
    2. Update the pid in the Additional WHERE clause field.
    3. Run the processor once (Right Click -> Run Once)
    4. Stop the processor after the data is read (If not already)
    5. Clear out the Table name and pid (with a placeholder)

**NOTE:**

STOP the processor after the data has been read from the table. 

Why stop the processor?

For debugging, `nifi_entry_id` is not configured into the processor which keeps track of the last row read from the table. Due to that, not stopping the processor will lead to the same row being read repeatedly.

To allow time for obeserving the flow, the processor is scheduled to execute only once every 10 minutes. This allows sufficient time to stop the processor. 

#### *Case 2: Data has been written to analytics*

The only failure possible here is failure to delete data from the camunda table.

Use the `GenerateFlowFile` processor with the title `(Debug Workflow) Enter pid and tablename`

Debugging Steps:

    1. Enter the `tablename` and `pid` values
    2. Run the processor once (Right Click -> Run Once)
    3. Stop the processor (if not already)
    4. Clear out the Table name and pid (with a placeholder)


### Nifi Registry

![Home/Controller Settings](images/Home%20-%20Hamburger%20Menu.png)

![Setting up Registry Client](images/Registry%20Clients.png)


### Logging