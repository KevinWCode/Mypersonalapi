# Setting up the spare computer as the server

Plan on about an hour the first time.

## 1. Install Ubuntu Server
- Download **Ubuntu Server 24.04 LTS** and write it to a USB stick with balenaEtcher or Rufus.
- Install it. Tick **"Install OpenSSH server"** so you can run the machine from your main computer.
- Plug it into the router with ethernet if you can. It's more reliable than Wi-Fi.

## 2. Make sure it stays on
- In the BIOS, set **"Restore on AC power loss" → Power On** so it boots itself after a power cut.
- On a laptop, keep it plugged in and stop it sleeping when the lid closes. Set `HandleLidSwitch=ignore` in `/etc/systemd/logind.conf`, then reboot.

## 3. Install Docker
```
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER   # log out and back in afterwards
```

## 4. Install Tailscale (lets your phone reach the server from anywhere, with no ports opened on your router)
```
curl -fsSL https://tailscale.com/install.sh | sh
sudo tailscale up
```
Install the Tailscale app on your phone and sign in with the same account. The server gets an address like `100.x.y.z`, which the phone app will use.

## 5. Run the Personal API
```
git clone https://github.com/KevinWCode/Mypersonalapi.git personal-api
cd personal-api
cp .env.example .env
nano .env        # fill in GITHUB_TOKEN and INGEST_TOKEN (see below)
docker compose up -d --build
curl localhost:8000/days
```

- **GITHUB_TOKEN**: on GitHub, go to Settings → Developer settings → Fine-grained tokens. Create a token with read-only access to all your repositories (Contents: read). It lets commits in private repos count too.
- **INGEST_TOKEN**: any long random string. `openssl rand -hex 32` makes one. The phone app will use the same value.

## 6. Backups
All data lives in `personal-api/data/personal.db`. Copying that one file somewhere else, like a USB stick or cloud drive, is a full backup.
