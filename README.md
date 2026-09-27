# StudentBook Setup Guide

Welcome to the StudentBook School Management ERP. Follow the steps below to set up and run the project locally on your machine.

## Prerequisites

Before you begin, ensure you have the following installed on your system:
- **Docker Desktop** (Make sure it is running)
- **Node.js** (v18 or higher)
- **Git**

---

## 1. Start the Backend and Database

The backend services and PostgreSQL database are fully containerized and managed via Docker Compose. This ensures you don't need to manually configure a database or install Java.

1. Open your terminal (PowerShell or Command Prompt).
2. Navigate to the root directory of the project (`StudentBook`).
3. Run the following command to build and start the containers in the background:

   ```bash
   docker compose up -d --build
   ```

   *Note: The first time you run this, it may take a few minutes to download the necessary Docker images and build the Spring Boot application.*

4. You can check the logs to ensure the backend started successfully:
   ```bash
   docker logs -f schoolerp_backend
   ```
   *(Wait until you see "Started SchoolErpApplication" in the logs. Press `Ctrl+C` to exit the logs).*

---

## 2. Start the Frontend

The frontend is built with React, Vite, and Tailwind CSS. 

1. Open a **new** terminal window.
2. Navigate to the `frontend` directory:
   ```bash
   cd frontend
   ```
3. Install the required Node dependencies:
   ```bash
   npm install
   ```
4. Start the Vite development server:
   ```bash
   npm run dev
   ```

5. Once the server starts, it will provide a local URL (usually `http://localhost:5173`). Open that URL in your web browser.

---

## 3. Login Credentials

The database is automatically seeded with an initial school profile, structure, and an administrator account when it starts up.

You can log in to the sample school's dashboard using the following credentials:

- **Username:** `admin`
- **Password:** `Admin@1234`

---

## 4. Registering a New School

Any school can sign up from the landing page (**Create Account**, or go to `/register`).
On success the owner is emailed:

- a **User ID** built from the school's short name or initials, e.g. `nsnvps.admin`
  (unique across the platform and permanent), and
- a **temporary password**, which must be changed at first login.

Everything entered at sign-up appears in **Settings → School Profile**. A current academic
year and the default fee categories are created automatically; classes are added in Settings.

**Forgot password:** the login page's *Forgot password?* link emails a 6-digit code (valid 10 minutes,
5 attempts) to the user's email, or to the school's registered email for its admin.

**Emails in development** go to **Mailpit**, a local mail catcher started by Docker Compose;
nothing is sent to real inboxes. Open **http://localhost:8025** to read them.
For production, set the `MAIL_*` variables in `.env` (see `.env.template`) to a real SMTP provider.

---

## Useful Commands

- **Stop the backend servers:**
  ```bash
  docker compose down
  ```
  
- **Wipe the database completely** (This will delete all data and re-run migrations from scratch next time you start):
  ```bash
  docker compose down -v
  ```

- **Restart just the backend API:**
  ```bash
  docker restart schoolerp_backend
  ```
