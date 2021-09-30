FROM docker-remote.artifacts.developer.gov.bc.ca/python:3.8

RUN apt-get update \
    && apt-get install -y git 

RUN git clone -b ${FORMIO_SOURCE_REPO_BRANCH} ${FORMIO_SOURCE_REPO_URL} /api/

WORKDIR /forms-flow-api/app
RUN cp -r /api/forms-flow-api/* /forms-flow-api/app
RUN USER 1001
RUN pip install --upgrade pip
RUN pip install nltk
RUN pip install -e .

RUN mkdir /nltk_data && chmod 777 /nltk_data
RUN python -m nltk.downloader punkt vader_lexicon subjectivity

EXPOSE 5000
RUN chmod u+x ./entrypoint
ENTRYPOINT ["/bin/sh", "entrypoint"]