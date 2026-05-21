FROM node:22-slim
WORKDIR /app

COPY package*.json ./
RUN npm install --no-fund --no-audit

COPY tsconfig.json vitest.config.ts ./
COPY tests/ tests/

ENV FLOCI_GCP_ENDPOINT=http://floci-gcp:4578
ENV FLOCI_GCP_PROJECT=test-project
ENV PUBSUB_EMULATOR_HOST=floci-gcp:4578
ENV FIRESTORE_EMULATOR_HOST=floci-gcp:4578
ENV DATASTORE_EMULATOR_HOST=floci-gcp:4578
ENV STORAGE_EMULATOR_HOST=http://floci-gcp:4578
ENV SECRET_MANAGER_EMULATOR_HOST=floci-gcp:4578
ENV TEST_RESULTS_DIR=/results

RUN mkdir -p /results
ENTRYPOINT ["npm", "test"]
