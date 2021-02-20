FROM python:3.8docker-remote.artifacts.developer.gov.bc.ca/python:3.8

RUN apt-get update \
    && apt-get install -y git 

RUN git -b v4.0.0-alpha clone https://github.com/AOT-Technologies/forms-flow-ai.git /api/

RUN cp /api/forms-flow-api/ /forms-flow-api/app
WORKDIR /forms-flow-api/app

RUN pip install --upgrade pip
RUN pip install psycopg2

RUN pip install .

RUN mkdir /nltk_data && chmod 777 /nltk_data

EXPOSE 5000
RUN chmod u+x ./entrypoint
ENTRYPOINT ["/bin/sh", "entrypoint"]
