# Building Serviceflow in Openshift
## Build Configs 
<br>
The yaml files in the build_configs folder are used to build Formsflow.AI with some custom updates required specifically for Service BC.  
<br><br>
Features include changes to look and feel as well as additional components to allow updating to our analytics server.
<br>

## Deployments Configs 
<br>
The yaml files in the deployment_configs folder are to deploy images build in the tools area to dev/test/production workspaces.
<br><br>
Example command: <br/>
oc process -f camunda_dc.yaml --param-file=camunda_param.yaml | wf-dev apply -f -
<br><br>
These templates are based on information provided in this repo: https://github.com/AOT-technologies/forms-flow-ai
<br><br>
The application requires additional applications to be setup to operate including: Redis, Postgres, MongoDB
<br><br>
Example templates for this can be found here: https://github.com/AOT-Technologies/forms-flow-ai/tree/master/deployment/openshift/Databases
