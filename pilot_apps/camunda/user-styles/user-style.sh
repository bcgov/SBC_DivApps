if [ "$1" = "dev" ] 
then
  cp ./user-styles/user-styles-dev.css ./src/main/resources/META-INF/resources/webjars/camunda/app/admin/styles/user-styles.css
  cp ./user-styles/user-styles-dev.css ./src/main/resources/META-INF/resources/webjars/camunda/app/cockpit/styles/user-styles.css
  cp ./user-styles/user-styles-dev.css ./src/main/resources/META-INF/resources/webjars/camunda/app/tasklist/styles/user-styles.css
  cp ./user-styles/user-styles-dev.css ./src/main/resources/META-INF/resources/webjars/camunda/app/welcome/styles/user-styles.css
  echo "---> dev env"
elif [ "$1" = "test" ] 
then
  cp ./user-styles/user-styles-test.css ./src/main/resources/META-INF/resources/webjars/camunda/app/admin/styles/user-styles.css
  cp ./user-styles/user-styles-test.css ./src/main/resources/META-INF/resources/webjars/camunda/app/cockpit/styles/user-styles.css
  cp ./user-styles/user-styles-test.css ./src/main/resources/META-INF/resources/webjars/camunda/app/tasklist/styles/user-styles.css
  cp ./user-styles/user-styles-test.css ./src/main/resources/META-INF/resources/webjars/camunda/app/welcome/styles/user-styles.css
  echo "---> test env"
else
  cp ./user-styles/user-styles-prod.css ./src/main/resources/META-INF/resources/webjars/camunda/app/admin/styles/user-styles.css
  cp ./user-styles/user-styles-prod.css ./src/main/resources/META-INF/resources/webjars/camunda/app/cockpit/styles/user-styles.css
  cp ./user-styles/user-styles-prod.css ./src/main/resources/META-INF/resources/webjars/camunda/app/tasklist/styles/user-styles.css
  cp ./user-styles/user-styles-prod.css ./src/main/resources/META-INF/resources/webjars/camunda/app/welcome/styles/user-styles.css
  echo "---> prod env"
fi