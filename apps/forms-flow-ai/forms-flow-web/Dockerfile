# base image
FROM node:12.2.0-alpine as build-stage

# set working directory
WORKDIR /forms-flow-web/app

# add `/app/node_modules/.bin` to $PATH
ENV PATH /app/node_modules/.bin:$PATH

RUN apk update && apk upgrade && \
    apk add --no-cache bash git openssh

# install and cache app dependencies
COPY package-lock.json /forms-flow-web/app/package-lock.json
COPY package.json /forms-flow-web/app/package.json
COPY forms-flow-util/package.json /forms-flow-web/app/forms-flow-util/package.json
#RUN npm install --silent
#RUN npm install react-scripts@3.0.1 -g --silent

COPY . /forms-flow-web/app/
RUN npm run build

#RUN set -x \
#	&& chmod -R 777 /forms-flow-web/app/

# start app
#CMD ["npm", "start", "--prefix", "/forms-flow-web/app"]

FROM nginx:1.17 as production-stage
RUN mkdir /app
COPY --from=build-stage /app/build /usr/share/nginx/html
COPY nginx.conf /etc/nginx/nginx.conf
EXPOSE 8080:8080
CMD ["nginx", "-g", "daemon off;"]
