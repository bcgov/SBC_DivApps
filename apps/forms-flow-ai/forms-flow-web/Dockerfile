# base image
FROM docker-remote.artifacts.developer.gov.bc.ca/node:12.2.0-alpine as build-stage

# Clone code
RUN git clone https://github.com/AOT-Technologies/forms-flow-ai.git /app

# set working directory
WORKDIR /app/forms-flow-web

# add `/app/node_modules/.bin` to $PATH
ENV PATH /app/forms-flow-web/node_modules/.bin:$PATH

RUN apk update && apk upgrade && \
    apk add --no-cache bash git openssh

# install and cache app dependencies
COPY package-lock.json /app/forms-flow-web/package-lock.json
COPY package.json /app/forms-flow-web/package.json
COPY forms-flow-util/package.json /app/forms-flow-web/forms-flow-util/package.json

# Remove logo and copy new logo
RUN rm /app/forms-flow-web/public/logo.svg
COPY /public/logo.svg /app/forms-flow-web/public/logo.svg
RUN npm install

#RUN npm install react-scripts@3.0.1 -g --silent
#COPY . /app/
RUN npm run build

FROM docker-remote.artifacts.developer.gov.bc.ca/nginx:1.17 as production-stage
RUN mkdir /app
COPY --from=build-stage /app/build /usr/share/nginx/html
COPY nginx.conf /etc/nginx/nginx.conf
EXPOSE 8080:8080
CMD ["nginx", "-g", "daemon off;"]