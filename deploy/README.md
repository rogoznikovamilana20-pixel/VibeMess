# Vibe Server Infrastructure

## TURN Server (coturn)

For WebRTC calls to work behind NAT/firewalls, you need a TURN server.

### Quick start

```bash
# 1. Edit turnserver.conf — set your server domain/IP and credentials
# 2. Run:
docker compose -f deploy/docker-compose.yml up -d
```

### Configuration

Edit `turnserver.conf`:

- `realm=vibe.app` — change to your domain
- `user=vibe:your-secret-password` — set a strong password
- `cert=/etc/coturn/certs/cert.pem` — optional TLS certs

### Verify TURN is working

Use Trickle ICE: https://webrtc.github.io/samples/src/content/peerconnection/trickle-ice/

Add `turn:your-server.com:3478` with username `vibe` and your password.

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
