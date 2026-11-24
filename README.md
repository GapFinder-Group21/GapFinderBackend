# GapFinder

GapFinder has three repositories:

- **[GapFinderBackend](https://github.com/GapFinder-Group21/GapFinderBackend)**: API, database and analytics dashboard (Spring Boot + PostgreSQL in Docker).
- **[GapFinderFrontEnd-Dart](https://github.com/GapFinder-Group21/GapFinderFrontEnd-Dart)**: mobile app (Flutter).
- **[front-kotlin](https://github.com/GapFinder-Group21/front-kotlin)**: mobile app (Kotlin).

For the app to work, three things must be running at the same time: **the database, the backend and the app.**

## What you need

- Docker Desktop (open and running)
- Flutter SDK and Android Studio (for the emulator or a phone)
- The Google credentials (`GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET`)

### About the Google credentials

These keys are **private** and are not in the repository. Ask a team member to send them to you privately (for example, by direct message).


## 1. Start the database and the backend

In the backend folder, copy `.env.example` to `.env` and put the real values (`.env` is never committed):

```env
GOOGLE_CLIENT_ID=your-client-id
GOOGLE_CLIENT_SECRET=your-client-secret
GOOGLE_REDIRECT_URI=http://localhost:8080/google/callback
JWT_SECRET=a-long-random-value
```

If you only keep the dummy values of `.env.example`, the backend still starts, but the **Google Calendar import does not work** (Google rejects the dummy keys). `GOOGLE_REDIRECT_URI` is the address where Google sends the browser after the login; for a public server it must be `https://<domain>/google/callback`. `JWT_SECRET` signs the login tokens: always use your own value in production.

Then run:

```bash
docker compose up -d --build
```

This starts the database, pgAdmin and the backend. The first time, the tables are created automatically and demo data is loaded.

Check that it works by opening http://localhost:8080/dashboard.html

**Demo user:** `laura.gomez@uniandes.edu.co` / `Gapfinder123` (all demo users use the password `Gapfinder123`).

### Running the backend from your IDE instead

Start only the database:

```bash
docker compose up -d db pgadmin
```

Then run the backend from your IDE. Add `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET` as environment variables in the run configuration, or Spring will not start.

### Useful commands

```bash
docker compose down        # stop everything (data is kept)
docker compose down -v     # stop everything and DELETE the database
```

Delete the database (`down -v`) when you want to start from scratch, for example after changing `data.sql`. The demo data only loads when the database is empty.

**pgAdmin:** http://localhost:5050 (`admin@gapfinder.com` / `admin`). To connect, add a server with host `db`, port `5432`, user `gapfinder`, password `gapfinder`.

## 2. Run the app

In the frontend folder, open `lib/core/api_config.dart` and set the backend address:

| Where you run the app | `baseUrl` |
|---|---|
| Android emulator | `http://10.0.2.2:8080` |
| Phone on the same Wi-Fi as the computer | `http://<COMPUTER-IP>:8080` |
| Phone connected by USB (see below) | `http://localhost:8080` |

To find the computer's IP on Windows, run `ipconfig` and copy the **IPv4 Address**.

Then run:

```bash
flutter pub get
flutter run
```

**Phone over USB:** run `adb reverse tcp:8080 tcp:8080` so the phone can reach the backend without Wi-Fi. You have to run it again every time you unplug the phone.

## Deploy (public server with HTTPS)

Plan: a small Linux server (Ubuntu 22.04/24.04, at least 2 GB of RAM) that runs the same `docker compose`, plus a Caddy container that gets the HTTPS certificate by itself. `docker-compose.prod.yml` changes the base file for production: the database and the app have **no published ports**, pgAdmin is **not started**, and Caddy is the only one that listens on ports 80 and 443.

1. In the provider firewall open **only** ports 22, 80 and 443. Point a domain (or a free DuckDNS subdomain) to the public IP of the server.
2. Install Docker and get the code:

```bash
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER      # then log out and log in again
git clone https://github.com/GapFinder-Group21/GapFinderBackend.git
cd GapFinderBackend
```

3. Create the `.env` file (start from `cp .env.example .env`) with the real `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `GOOGLE_REDIRECT_URI=https://<domain>/google/callback`, `DOMAIN=<domain>` and a **new** `JWT_SECRET` (`openssl rand -base64 48`).
4. Start everything:

```bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build
docker compose -f docker-compose.yml -f docker-compose.prod.yml logs -f app   # wait for "Started GapfinderApplication"
```

5. Check it: `curl -i https://<domain>/` must answer (even a 401 means the request arrived), and the app must work from a phone with mobile data.
6. The same Google redirect address must be registered in Google Cloud Console (Credentials > OAuth client > Authorized redirect URIs).

Data lives in the Docker volumes `gapfinder-postgis-data` (database), `gapfinder-uploads` (open table photos) and `caddy-data` (certificates).
