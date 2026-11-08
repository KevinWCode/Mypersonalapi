# Connecting Google Calendar

The server reads your calendar read-only: it can see events but never create, change or delete them. You do
these steps once. Commands are run in `C:\Users\kevin\personal-api`.

## 1. Make a Google "app" for yourself (about 5 minutes)

1. Go to https://console.cloud.google.com and sign in with your Google account (kevin07502@gmail.com).
2. At the top, open the project picker, then **New project**. Name it `Personal API` and create it.
3. Search the top bar for **Google Calendar API**, open it and click **Enable**.
4. Search for **Google Auth Platform** (it may be called **OAuth consent screen**) and click **Get started**.
   App name `Personal API`, your email as support email, Audience **External**, your email as contact. Finish.
5. Open **Audience** and click **Publish app**. If you skip this, the sign-in stops working after 7 days.
6. Open **Clients**, then **Create client**. Application type **Desktop app**, name `Personal API server`,
   **Create**. Copy the **Client ID** and **Client secret**.

## 2. Give them to the server

Add to `.env`:

```
GOOGLE_CLIENT_ID=<the client ID>
GOOGLE_CLIENT_SECRET=<the client secret>
```

Install the new packages once: `.venv\Scripts\python -m pip install -r requirements.txt`

## 3. Sign in once

Run `.venv\Scripts\python -m app.google_auth`. A browser opens:

1. Pick your account.
2. Google says it hasn't verified the app. That's expected, because the app is yours: click **Advanced**, then
   **Go to Personal API (unsafe)**.
3. Allow **See events on all your calendars**. The tab then says "Signed in".

The sign-in is saved in `data\google_token.json`. Keep that file private. Then restart the server as usual;
blocks show on the dashboard after the next collect (up to an hour).

To undo it, delete `data\google_token.json` and remove "Personal API" at https://myaccount.google.com/permissions.
