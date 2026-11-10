# Deploying StudentBook for beta (free tier)

**Hosting plan:**
- **Render (free):** one web service runs the backend, with the React app bundled inside, at `https://<name>.onrender.com`.
- **Neon (free):** PostgreSQL database.
- **Brevo (free):** emails (sign-up credentials, verification codes), at 300 emails/day.

**Free-tier limits to expect:**

| | |
|---|---|
| Sleeps after inactivity | After **15 minutes** without traffic the app sleeps; the next visit takes **about 1 minute** to wake it. See step 6 to keep it awake. |
| Monthly hours | 750 free instance hours/month: enough for one always-on service. |
| Restarts | Render may restart free services at any time. In-memory rate-limit counters reset; data is safe in Neon. |
| Email | Render's free plan **blocks SMTP**, so the app sends mail through Brevo's HTTPS API instead. |

---

## 1. Put the code on GitHub (private repo)

1. **Create the repo.** On github.com, click **New repository** → name `studentbook` → **Private** → *don't* add a README.
2. **Push the code.** From `D:\Vibe Coding\StudentBook`:
   ```bash
   git remote add origin https://github.com/<your-username>/studentbook.git
   git push -u origin master
   ```

## 2. Database: Neon

1. **Create the project.** Sign up at **neon.tech** → **New project** → name `studentbook`, Postgres **16**, region **AWS Asia Pacific (Singapore)**.
2. **Get the connection details.** **Dashboard → Connect**. Pick the **pooled** connection and **Java (JDBC)**, and copy:
   - **Host:** `ep-xxxx-pooler.ap-southeast-1.aws.neon.tech`
   - **Database:** usually `neondb`
   - **User and password.**
3. **Build your `DATABASE_URL`:**
   ```
   jdbc:postgresql://<host>/<database>?sslmode=require
   ```
   Keep the user and password separate; they go in `DATABASE_USERNAME` / `DATABASE_PASSWORD`.

The tables are created automatically by Flyway the first time the app starts.

## 3. Email: Brevo

1. **Sign up** at **brevo.com** (free plan).
2. **Verify your sender address.** Go to **Senders, Domains & Dedicated IPs → Senders → Add a sender**, enter the address emails should come from (e.g. your Gmail), and click the verification link Brevo sends you.
3. **Create an API key.** Go to **SMTP & API → API Keys → Generate a new API key**. Choose **Restricted** access and set **Transactional email** to **All**. *Read* can't send, and the app then fails with HTTP 403. Copy the key; it is shown once.
4. **Decide your `MAIL_FROM`:** it must use the verified address, e.g. `StudentBook <yourname@gmail.com>`.

> **Deliverability:** without your own domain, emails sent "from" a Gmail address via Brevo may land in spam. Ask beta testers to check spam, and mark the first email "Not spam". Adding a domain later fixes this.

## 4. JWT secret

A random secret has already been generated for you in **`.env.production`** (git-ignored, on your computer only), alongside placeholders for the Neon and Brevo values. To make a new one yourself, generate a random secret of at least 64 characters (the app refuses shorter or example values). In PowerShell:

```powershell
-join ((48..57)+(65..90)+(97..122) | Get-Random -Count 80 | ForEach-Object {[char]$_})
```

Copy the output. **Never commit it or share it.**

## 5. Render

1. **Sign up** at **render.com** with your GitHub account.
2. **Create the Blueprint.** Click **New → Blueprint** → select the `studentbook` repo. Render reads `render.yaml`.
3. **Fill in the secrets** it asks for:

   | Key | Value |
   |---|---|
   | `JWT_SECRET` | from step 4 |
   | `DATABASE_URL` | from step 2 |
   | `DATABASE_USERNAME` | Neon user |
   | `DATABASE_PASSWORD` | Neon password |
   | `BREVO_API_KEY` | from step 3 |
   | `MAIL_FROM` | from step 3 |
4. **Deploy.** Click **Apply**. The first build takes ~5–10 minutes (it builds the frontend and backend).
5. **Wait for it to go live.** When the service shows **Live**, open `https://<name>.onrender.com`. Your landing page should load.

Links in emails automatically use the Render address (`RENDER_EXTERNAL_URL`).

## 6. Optional: keep it awake

To avoid the 1-minute wake-up during beta hours, ping it every 10 minutes:

1. **Sign up** at **cron-job.org** (free).
2. **Create the cronjob** for URL `https://<name>.onrender.com/api/health`, every 10 minutes.

## 7. First checks after deploy

1. **Sign-up email:** register a test school with an email you can read. Confirm the email arrives (check spam) and you can log in.
2. **Forgot password:** use "Forgot password?" with that email, and confirm the code arrives.
3. **Reload:** reload on `/dashboard` and confirm you stay signed in.

**About the sample school:** the database migrations also create the demo school *New Shanti Niketan Vidyapeeth*. Its default password is disabled on fresh databases (migration V16). To use it, run "Forgot password?" with the school's email address; this only works if you can receive mail there. Otherwise just register your own school.

## Updating the live app

Push to `master`. Render rebuilds and redeploys automatically (about 5–10 minutes), and database migrations run on startup.

## Troubleshooting

| Symptom | Likely cause |
|---|---|
| Deploy fails: "JWT_SECRET ..." | Secret missing, shorter than 64 characters, or one of the example values |
| Deploy fails: connection refused / SSL | `DATABASE_URL` missing `?sslmode=require`, or wrong host/user/password |
| Sign-up says "couldn't send the confirmation email" | Wrong `BREVO_API_KEY`, or `MAIL_FROM` isn't a verified Brevo sender. Check the Render **Logs** for "Brevo returned HTTP …" |
| Logs show `Brevo returned HTTP 403 … not authorized for resource transactional.email` | The API key's *Transactional email* permission is *Read*; edit the key in Brevo and set it to **All** (no need to change Render) |
| Site loads slowly the first time | Free instance was asleep (see step 6) |
| "Too many attempts" for everyone | Rate limiter can't see real client IPs; check logs for the IP on "Rate limit hit" lines |
