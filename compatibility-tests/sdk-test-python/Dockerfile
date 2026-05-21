FROM python:3.12-slim
WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY conftest.py pytest.ini ./
COPY tests/ tests/

ENV FLOCI_GCP_ENDPOINT=http://floci-gcp:4578
ENV FLOCI_GCP_PROJECT=test-project
ENV PUBSUB_EMULATOR_HOST=floci-gcp:4578
ENV FIRESTORE_EMULATOR_HOST=floci-gcp:4578
ENV DATASTORE_EMULATOR_HOST=floci-gcp:4578
ENV STORAGE_EMULATOR_HOST=http://floci-gcp:4578
ENV SECRET_MANAGER_EMULATOR_HOST=floci-gcp:4578

RUN mkdir -p /results
ENTRYPOINT ["pytest", "tests/", "-v", "--junit-xml=/results/junit.xml"]
