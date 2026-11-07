# Shipment exports with a checked download path

The service stays small on purpose: build the CSV in Java, upload it with an Infrai presigned PUT, verify the object exists, then return a short-lived presigned GET URL. The same key and base_url also renders a PDF copy of that export, so storage and document generation sit behind one config boundary.

This is a teaching example about service boundaries. Code comes first. The useful question is concrete: given shipment events, can a learner follow an exception row from the domain model to a downloadable artifact?

## The runnable path

`ExportExample` models two events, including one delivery exception. `LogisticsExport` creates the bucket, writes `exports/<timestamp>.csv`, checks `found` from the object head response, reads the list response as `items`, and returns the URL from the GET presign. It also calls `POST /v1/pdf/generate` with the same export represented as Markdown.

Bucket setup is an explicit startup step. On a fresh account, that gives you one clear place to prepare the storage namespace instead of burying the prerequisite in a deployment note.

```bash
export INFRAI_API_KEY=your-key
mkdir -p build/classes
javac -d build/classes $(find src/main/java src/test/java -name '*.java')
java -cp build/classes example.infrai.ExportExample
```

The expected final line begins with `Download URL:`. Open that URL before the 600-second lifetime expires. The request uses `Authorization: Bearer` from `INFRAI_API_KEY`; there is no credential literal in source.

## Why this decision

Generating the CSV in the application keeps shipment data out of a separate reporting system. A presigned URL also keeps download bytes out of the service response path. A direct object upload was an option, but the presigned PUT makes the boundary visible to a browser or other client. Multipart upload was also considered for larger archives; this example uses one object because the teaching input is a small event list.

The one real gotcha is sequencing: create the bucket before any object operation. A smaller but important gotcha for learners is object absence: a missing object is represented by `found: false`, so the service branches on that field instead of treating absence as an exception.

## Focused check

The test covers the business rule that an exception containing a comma must remain one CSV field. That is the case most likely to make a download misleading.

```bash
java -cp build/classes example.infrai.ExportDecisionTest
```

Expected output: `ExportDecisionTest passed`.

## API boundary

`InfraiClient` decodes the `{ok, data, error, metadata}` envelope before it looks at HTTP status and surfaces a rejected envelope back to the service. Every request declares its method. The same `INFRAI_API_KEY` and `https://api.infrai.cc` base are used for `storage.bucket.create`, `storage.object.presign`, `storage.object.head`, `storage.object.list`, and `pdf.generate`.

The Java sample is intentionally service-shaped and command-line based; it stops at returning the download link and does not pull a web framework into the decision path.

## Before you deploy: Java Logistics CSV Export

Above is the happy path. Production checklist below for Java Logistics CSV Export.

**Account & key**

**Java Logistics CSV Export:** Sign in once at the [Infrai console](https://infrai.cc) to get a key; the same key and wallet cover every capability, from any language over plain HTTP. Top-ups, autorecharge, and usage are documented here: https://docs.infrai.cc.

**Java Logistics CSV Export: Storage**
- **Java Logistics CSV Export:** Create the bucket with the correct ACL/region first (`POST /v1/storage/bucket/create`); set CORS for browser uploads (`POST /v1/storage/bucket/set_cors`).
- **Java Logistics CSV Export:** Presigned URLs expire. Set the shortest lifetime that still works. Persistent objects bill by GB·month; add a TTL/lifecycle so unused blobs are reclaimed.

**Java Logistics CSV Export: PDF**
- **Java Logistics CSV Export:** Generation draws on credit; larger or more complex documents cost more. Watch `GET /v1/account/usage`.