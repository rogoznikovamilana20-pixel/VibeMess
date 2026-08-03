# Vibe Server Infrastructure

## TURN Server (coturn)

For WebRTC calls to work behind NAT/firewalls, you need a TURN server.

### Architecture

The TURN secret never leaves the server:

1. **coturn** runs with `use-auth-secret` (see `turnserver.conf`), secret = `static-auth-secret`
2. **vibe-server** issues short-lived credentials: `GET /api/turn/credentials` (requires JWT auth) returns
   `{ urls: [turn:your-server.com:3478], username: <unix-expiry>, credential: base64(HMAC-SHA1(TURN_SECRET, username)), ttl: 3600 }`
3. **App** requests credentials right before each call; if the server is unreachable it falls back to the public openrelayproject TURN

### Quick start

```bash
# 1. In vibe-server/.env set:
#    TURN_SECRET=<long random string>
#    TURN_SERVER_URL=turn:your-server.com:3478
# 2. In turnserver.conf set the SAME value as static-auth-secret (plus external-ip/relay-ip on the VPS)
# 3. Run:
docker compose -f deploy/docker-compose.yml up -d
```

### Verify TURN is working

Use Trickle ICE: https://webrtc.github.io/samples/src/content/peerconnection/trickle-ice/

Add `turn:your-server.com:3478` with the username/password returned by `GET /api/turn/credentials` (open port 3478 UDP/TCP in the firewall).

## Supabase

The app uses Supabase Realtime for WebRTC signaling.

1. Create a project at https://supabase.com
2. Go to Project Settings → API
3. Copy `Project URL` and `anon public key`
4. Add to `local.properties`:

```properties
SUPABASE_URL=https://your-project-id.supabase.co
SUPABASE_ANON_KEY=your-anon-key
```

No tables need to be created — the signaling uses Supabase Realtime's built-in channels.

## CI/CD

### GitHub Secrets

Set these in your GitHub repository: Settings → Secrets and variables → Actions

| Secret | Description |
|--------|-------------|
| `SUPABASE_URL` | Supabase project URL |
| `SUPABASE_ANON_KEY` | Supabase anon public key |
| `AI_API_KEY` | OpenAI API key (optional, for Aurion AI) |

### Releasing

Push a tag to trigger automatic APK build + GitHub Release:

```bash
git tag v1.0.0
git push origin v1.0.0
```
