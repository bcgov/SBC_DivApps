# Nifi Documentation

Outline:

## Setup

### Nifi Configuration

#### Controller Services
![Controller Services](./images/Controller%20Services.png)

#### Parameter Context
![Parameter Context](./images/Parameter%20Context.png)

![Parameter Context](./images/Parameters.png)

Following are the parameters
- 

### Camunda POSTGRES setup

### Nifi Flow Description

### How to debug?

Also, debugging is very straightforward where all you need to do is feed the pid and table name (both of which are available in the error notifications). There are two debug workflows:

At the very beginning; 

At the very end; delete from postgres- This can be used if the data has been written to analytics but delete from postgres was unsuccessful (Mainly useful if you don't want to connect to prod postgres from your machine)

- (Debug Workflow) Query Any Table - Edit before execution
- **Processor**: QueryDatabaseTable

This processor is exclusively set up for debugging. 

This processor MUST be edited before starting.

Prior to starting this processor,

1) Update the Table Name field
2) Update the pid in the Additional WHERE clause field.
3) STOP after the read event is successful
4) Clear out the Table name and pid with a placeholder.

Because this is exclusive for debugging, a nifi_entry_id  is not set up. To allow time for Stopping the processor, the processor is configured to execute only every 10 minutes which should allow you to verify read and stop execution.

### Nifi Registry

![Home/Controller Settings](./images/Home%20-%20Hamburger%20Menu.png)

![Setting up Registry Client](./images/Registry%20Clients.png)


### Logging