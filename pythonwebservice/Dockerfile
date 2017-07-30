FROM python:3.6.1-alpine
COPY requirements.txt /tmp
RUN apk add --no-cache libpq libstdc++ \
    && apk add --no-cache --virtual buildreq g++ gcc musl-dev linux-headers libffi-dev jpeg-dev postgresql-dev \
    && pip install --no-cache-dir -r /tmp/requirements.txt \
    && apk del buildreq
COPY . /tmp/app
RUN pip install /tmp/app
CMD ["webserver.py"]
