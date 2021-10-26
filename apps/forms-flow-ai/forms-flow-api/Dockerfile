FROM docker-remote.artifacts.developer.gov.bc.ca/python:3.8

ENV PATH=/venv/bin:$PATH

RUN : \
    && python3 -m venv /venv \
    && pip install --upgrade pip \
    && pip install -r requirements.txt 

ADD . /forms-flow-api/app

RUN pip install .

EXPOSE 5000

RUN chmod u+x ./entrypoint
ENTRYPOINT ["/bin/sh", "entrypoint"]