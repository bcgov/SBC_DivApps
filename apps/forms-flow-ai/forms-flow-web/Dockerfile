# base image
FROM docker-remote.artifacts.developer.gov.bc.ca/node:12.2.0-alpine as build-stage

# set working directory
WORKDIR /app

# add `/app/node_modules/.bin` to $PATH
ENV PATH /app/node_modules/.bin:$PATH

RUN apk update && apk upgrade && \
    apk add --no-cache bash git openssh

# Clone code
RUN git clone -b v4.0.0-alpha  https://github.com/AOT-Technologies/forms-flow-ai.git /web/

# install and cache app dependencies
RUN cp /web/forms-flow-web/package-lock.json /app/package-lock.json
RUN cp /web/forms-flow-web/package.json /app/package.json
RUN mkdir /app/forms-flow-util
RUN cp /web/forms-flow-web/forms-flow-util/package.json /app/forms-flow-util/package.json

# RUN npm install react-scripts@3.0.1 -g --silent

# Remove logo and copy new logo and code
RUN rm /web/forms-flow-web/public/logo.svg
COPY /public/logo.svg /web/forms-flow-web/public/logo.svg
RUN npm install
RUN cp -r /web/forms-flow-web/* /app/
RUN npm run build

FROM docker-remote.artifacts.developer.gov.bc.ca/nginx:1.17 as production-stage
RUN mkdir /app
COPY --from=build-stage /app/build /usr/share/nginx/html
COPY nginx.conf /etc/nginx/nginx.conf
EXPOSE 8080:8080
CMD ["nginx", "-g", "daemon off;"]