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

In the backend folder, create a file called `.env` with the Google credentials:

```env
GOOGLE_CLIENT_ID=your-client-id
GOOGLE_CLIENT_SECRET=your-client-secret
```

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


