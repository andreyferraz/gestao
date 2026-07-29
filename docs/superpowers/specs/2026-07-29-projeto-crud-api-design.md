# Projeto CRUD REST API Design

## Goal

Complete the Projeto CRUD and expose a REST API that can be read publicly by
the portfolio at `https://www.andreyferraz.com.br`, while keeping creation,
editing, and deletion restricted to administrators of this application.

The administration screen itself is outside this scope. The protected REST
operations will provide the backend contract for that screen later.

## API Contract

The API base path is `/api/projetos`.

| Method | Path | Access | Request | Response |
| --- | --- | --- | --- | --- |
| `GET` | `/api/projetos` | Public | None | `200` with a JSON array |
| `GET` | `/api/projetos/{id}` | Public | None | `200` with one project or `404` |
| `GET` | `/api/projetos/imagens/{arquivo}` | Public | None | `200 image/webp` or `404` |
| `POST` | `/api/projetos` | `ADMIN` | `multipart/form-data` | `201`, `Location`, and created project |
| `PUT` | `/api/projetos/{id}` | `ADMIN` | `multipart/form-data` | `200` with updated project or `404` |
| `DELETE` | `/api/projetos/{id}` | `ADMIN` | None | `204` or `404` |

The multipart fields are:

- `titulo`: required, non-blank string.
- `descricao`: required, non-blank string.
- `link`: required HTTP or HTTPS URL.
- `imagem`: required on creation and optional on update.

Clients cannot assign `id` or `imagemUrl`. These values are controlled by the
application.

JSON responses contain:

```json
{
  "id": "54ea9c87-d07c-4b86-9aaf-63307300231e",
  "titulo": "Project title",
  "descricao": "Project description",
  "imagemUrl": "https://api.example.com/api/projetos/imagens/54ea9c87-d07c-4b86-9aaf-63307300231e.webp",
  "link": "https://example.com"
}
```

`imagemUrl` is assembled from the current request origin and the stored,
application-generated filename. The database continues to store only the
filename.

## Security and CORS

The existing form login remains enabled. HTTP Basic is enabled as an
alternative authentication mechanism for API clients.

Authorization rules:

- The three public `GET` endpoints under `/api/projetos` use `permitAll`.
- `POST`, `PUT`, and `DELETE` under `/api/projetos` require role `ADMIN`.
- Existing application endpoints keep their current authenticated behavior.

CSRF remains enabled. This protects the future administration screen when it
uses the existing form-login session. A non-browser HTTP Basic client that
performs a mutation must also provide a valid CSRF token.

CORS allows `GET`, `HEAD`, and `OPTIONS` from exactly
`https://www.andreyferraz.com.br`. The allowed origin is backed by an
application property so development and deployment can override it without a
code change. Cross-origin mutation methods are not allowed.

## Persistence

`Projeto.id` remains a UUID stored in the SQLite `TEXT` primary-key column.
Creation generates `UUID.randomUUID()` in the service and calls an explicit
repository `INSERT`, following the established Lead and Vendedor pattern.
This avoids `CrudRepository.save` treating an assigned UUID as an existing
entity and avoids asking SQLite to generate a UUID.

Updates use an explicit repository `UPDATE`. Reads and existence checks use
the Spring Data repository operations. Service methods are transactional for
database consistency.

The schema marks `titulo`, `descricao`, `imagem_url`, and `link` as `NOT NULL`
for new databases. Service validation remains the authoritative guard for
existing SQLite databases created with the previous nullable definition.

## Image Storage

Uploads are converted to WebP and stored below the configured `upload.dir`.
The application includes the `com.github.usefulness:webp-imageio` runtime
encoder. The existing `cwebp` command remains a compatibility fallback.

Before processing, the service rejects:

- Null or empty multipart content.
- Content that cannot be decoded as an image.
- Unsupported filenames or missing original filenames.

All generated filenames use the pattern `<UUID>.webp`.

All file resolution normalizes the configured upload root and candidate path,
then verifies the candidate remains below the root. Public reads and deletion
also require the generated filename pattern. This prevents path traversal.

File lifecycle rules:

- Creation uploads first. If the database insert fails, the newly uploaded
  file is removed.
- Update without a file preserves the existing filename.
- Update with a file uploads the replacement, persists its filename, and only
  then removes the old file. If persistence fails, the replacement is removed
  and the old file remains.
- Delete removes the database row and then its image. A file-removal failure is
  surfaced rather than silently ignored.
- A failed conversion removes any partial destination file.

The filesystem cannot participate in the JDBC transaction, so these explicit
compensation rules reduce orphaned files and preserve the prior image across
expected persistence failures.

## Validation and Errors

The controller uses a request DTO rather than binding the persistence entity.
Bean Validation and service validation reject blank values. `link` accepts
only absolute `http` or `https` URLs.

Error semantics:

- Invalid fields, invalid UUIDs, missing required multipart fields, and invalid
  images return `400`.
- Missing projects or image files return `404`.
- Unauthenticated mutations return `401`.
- Authenticated users without role `ADMIN` receive `403`.
- Unexpected persistence or filesystem failures return the existing generic
  `500` response and are logged.

The existing `GlobalExceptionHandler` remains the shared JSON error format.
Projeto not-found conditions use `NoSuchElementException` so they map to
`404`.

## Components

- `Projeto`: Spring Data JDBC persistence model.
- `ProjetoRepository`: explicit insert/update plus CRUD reads and deletes.
- `ProjetoService`: validation, UUID ownership, transactions, and compensated
  image lifecycle.
- `ProjetoRequest`: mutable multipart request DTO with validation annotations.
- `ProjetoResponse`: public response DTO with the externally usable image URL.
- `ProjetoController`: REST routing, status codes, multipart binding, and
  conversion to responses.
- `FileUploadService`: safe WebP storage, lookup, and removal.
- `SecurityConfig`: public-read/admin-write rules, HTTP Basic, and CORS.

## Testing Strategy

Implementation follows test-driven development.

Service tests cover:

- Creation assigns a UUID and compensates when persistence fails.
- Blank fields and invalid links are rejected.
- Update preserves an existing image when no new file is supplied.
- Update replaces and cleans up images in the correct order.
- Update compensation preserves the old image on database failure.
- Delete removes the row and image.
- Missing IDs produce `NoSuchElementException`.

Repository integration tests prove UUID insertion, update, listing, lookup,
and deletion against a real test database. Test database identifier handling
must match Spring Data's quoted lowercase table names.

Controller and security tests cover:

- Public list, detail, and image reads.
- `404` behavior.
- Multipart creation and update.
- `401` for unauthenticated mutations.
- `403` for non-admin mutations.
- Successful admin mutations.
- CORS acceptance for `https://www.andreyferraz.com.br`.
- CORS rejection for other origins and all cross-origin mutation methods.

File tests use a temporary upload directory and cover valid conversion,
invalid image rejection, safe lookup/removal, traversal rejection, and partial
file cleanup.

The final verification runs the focused Projeto tests and the complete Maven
test suite with JDK 17.

## Non-Goals

- Building the administration HTML form.
- Adding pagination, project ordering, tags, or publication status.
- Moving images to cloud/object storage.
- Providing credentials to the public JavaScript application.
