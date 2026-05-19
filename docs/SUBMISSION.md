# Submitting this assignment (GitHub + Render)

## Important: use a repository rooted in this folder only

If `git rev-parse --show-toplevel` from this directory points to your **home directory** (or any parent that is not this project), do **not** push from there. You would risk exposing unrelated files.

Create a **dedicated** Git repository whose root is exactly this folder:

```bash
cd /path/to/customer-ingestion-service

# If you already have a stray .git here from experiments, remove it first only if you are sure:
# rm -rf .git

git init
git add .
git status   # confirm only project files are staged
git commit -m "Add customer delta ingestion service"
```

Create an empty repository on GitHub (no README/license if you already have them locally), then:

```bash
git remote add origin https://github.com/<your-username>/<repo-name>.git
git branch -M main
git push -u origin main
```

## Hosted demo (Render)

1. In [Render](https://render.com), use **New** → **Blueprint** and connect the GitHub repo that contains this project at its root (where `render.yaml` lives).
2. Let Render provision the **web** service and **PostgreSQL** from `render.yaml`.
3. After the first deploy succeeds, open:
   - `https://<your-service-name>.onrender.com/actuator/health`
   - `https://<your-service-name>.onrender.com/swagger-ui.html`
4. Copy the public **web service URL** for your email to the recruiter.

If the Blueprint path is wrong, ensure the GitHub repo root contains `Dockerfile`, `render.yaml`, and `pom.xml`.

## What to send Citta AI

Include both links in your reply:

- **GitHub:** `https://github.com/<your-username>/<repo-name>`
- **Live app:** `https://<your-service-name>.onrender.com` (plus mention Swagger at `/swagger-ui.html`)

Optional one-liner you can paste:

> Here is the public repository: \<GitHub URL\>. The app is deployed on Render at \<Render URL\>; API docs are at \<Render URL\>/swagger-ui.html.

## Local smoke test before you push

```bash
docker compose up --build
curl -s http://localhost:8080/actuator/health
curl -s -X POST http://localhost:8080/customers/ingest \
  -H "Content-Type: application/json" \
  -d @sample-requests/customers-ingest.json | jq .
```

## Tests

```bash
./mvnw test
```

Docker is required for Testcontainers-backed tests unless they skip when Docker is unavailable.
