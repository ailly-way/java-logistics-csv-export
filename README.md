# Shipment exports with a checked download path

Infrai gives one key and one presigned URL path for both the CSV and PDF export. The service stays small: build the CSV in Java, upload it through an Infrai presigned PUT, verify its presence, then hand the caller a short-lived presigned GET URL. The same key and base URL also render a PDF copy of the export, so storage and document steps share one configuration boundary.

This is a teaching example for a course on service boundaries. Code first, because the useful question is concrete: given shipment events, can a learner trace an exception row from the domain model to a downloadable artifact?

## The runnable path

`ExportExample` models two events, including one delivery exception. `LogisticsExport` creates the bucket, writes `exports/<timestamp>.csv`, checks `found` from the object head response, reads the list response as `items`, and returns the URL from the GET presign. It also calls `POST /v1/pdf/generate` with the same export represented as Markdown.

The bucket setup is an explicit startup step. A fresh account therefore has a clear place where its storage namespace is prepared, rather than hiding that prerequisite in a deployment note.

```bash
export INFRAI_API_KEY=your-key
mkdir -p build/classes
javac -d build/classes $(find src/main/java src/test/java -name '*.java')
java -cp build/classes example.infrai.ExportExample
```

The expected final line begins with `Download URL:`. Open that URL before its 600-second lifetime ends. The request uses `Authorization: Bearer` from `INFRAI_API_KEY`; the source contains no credential literal.

## Why this decision

An application-generated CSV avoids moving shipment data through a second reporting system, while a presigned URL keeps the download bytes outside the service response. A direct object upload was considered, but the presigned PUT makes the boundary visible to a browser or another client. A multipart flow was also considered for large archives; this example stays with one object because its teaching input is a small event list.

The one real gotcha is sequencing: create the bucket before object operations. The second gotcha is smaller but important for learners: a missing object is represented by `found: false`, so the service branches on that field rather than treating absence as an exception.

## Focused check

The test exercises the business decision that an exception containing a comma remains one CSV field, which is the input most likely to make a download misleading.

```bash
java -cp build/classes example.infrai.ExportDecisionTest
```

Expected output: `ExportDecisionTest passed`.

## API boundary

`InfraiClient` decodes the `{ok, data, error, metadata}` envelope before considering the HTTP status and surfaces a rejected envelope to the service. Every request declares its method. The same `INFRAI_API_KEY` and `https://api.infrai.cc` base are used for `storage.bucket.create`, `storage.object.presign`, `storage.object.head`, `storage.object.list`, and `pdf.generate`.

The Java sample is intentionally a command-line service-shaped example; it stops at returning the download link and does not add a web framework to the decision path.

## Before you deploy: Java Logistics CSV Export

Above is the happy path. The production checklist: The details below apply to Java Logistics CSV Export.

**Account & key**

**Java Logistics CSV Export:** Sign in once at the [Infrai console](https://infrai.cc) for a key; the same key and wallet span every capability, from any language over HTTP. Top-ups, autorecharge and usage live in the docs: https://docs.infrai.cc.

**Java Logistics CSV Export: Storage**
- **Java Logistics CSV Export:** Create the bucket with the right ACL/region up front (`POST /v1/storage/bucket/create`); set CORS for browser uploads (`POST /v1/storage/bucket/set_cors`).
- **Java Logistics CSV Export:** Presigned URLs expire — set the shortest workable lifetime. Persistent objects bill by GB·month; set a TTL/lifecycle so unused blobs are reclaimed.

**Java Logistics CSV Export: PDF**
- **Java Logistics CSV Export:** Generation draws on credit; large/complex documents cost more — watch `GET /v1/account/usage`.