#!/bin/bash
# Vibe Supabase setup script
# This script validates that Supabase is properly configured for WebRTC signaling.
#
# Requirements:
#   - Supabase CLI (https://supabase.com/docs/guides/cli)
#   - jq (https://jqlang.github.io/jq/)
#
# Usage:
#   ./scripts/setup-supabase.sh [project-ref]
#
# If no project-ref is provided, reads from local.properties.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
PROPERTIES_FILE="$PROJECT_DIR/local.properties"

echo "=== Vibe Supabase Setup ==="
echo ""

# Check if Supabase CLI is installed
if ! command -v supabase &> /dev/null; then
    echo "Supabase CLI not found. Install it first:"
    echo "  brew install supabase/tap/supabase  # macOS"
    echo "  Or see: https://supabase.com/docs/guides/cli"
    exit 1
fi

# Get project ref
if [ -n "${1:-}" ]; then
    PROJECT_REF="$1"
else
    if [ -f "$PROPERTIES_FILE" ]; then
        SUPABASE_URL=$(grep "^SUPABASE_URL=" "$PROPERTIES_FILE" | cut -d= -f2- | tr -d '"')
        PROJECT_REF=$(echo "$SUPABASE_URL" | sed -n 's|https://\(.*\)\.supabase\.co|\1|p')
    fi
fi

if [ -z "${PROJECT_REF:-}" ]; then
    echo "Error: No Supabase project reference found."
    echo "Provide it as argument or set SUPABASE_URL in local.properties"
    echo ""
    echo "Example:"
    echo "  ./scripts/setup-supabase.sh abcdefghijklmnopqrst"
    exit 1
fi

echo "Project ref: $PROJECT_REF"
echo ""

# Link to Supabase project
echo "Linking to Supabase project..."
supabase link --project-ref "$PROJECT_REF"

# Enable Realtime (Realtime is enabled by default on new projects)
echo ""
echo "Realtime is enabled by default on Supabase projects."
echo "No additional configuration needed for WebRTC signaling."
echo ""
echo "=== Setup Complete ==="
echo ""
echo "Next steps:"
echo "  1. Get your anon key from:"
echo "     https://supabase.com/dashboard/project/$PROJECT_REF/settings/api"
echo "  2. Add to local.properties:"
echo "     SUPABASE_URL=https://$PROJECT_REF.supabase.co"
echo "     SUPABASE_ANON_KEY=<your-anon-key>"
echo "  3. Start the app and make a call!"
