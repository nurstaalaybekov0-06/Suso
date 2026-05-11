# Deploy

This Spring Boot app needs a server because it has login, sessions, and a database.
To use it without `localhost`, deploy it to a hosting provider such as Render.

## Render

1. Push this project to GitHub.
2. Open Render and create a new Blueprint.
3. Select the repository with this project.
4. Render will read `render.yaml`, create a web service and a PostgreSQL database.
5. After deploy finishes, open the public Render URL.

The app uses `SPRING_PROFILES_ACTIVE=postgres` on Render and reads the database
settings from environment variables defined in `render.yaml`.
