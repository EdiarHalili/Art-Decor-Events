# Art Decor Workforce App Installation

The web app is installable as a Progressive Web App on Android and iPhone when served over HTTPS. Localhost is also accepted for development testing.

## Android

1. Open Chrome on the phone.
2. Visit the app URL, for example `https://your-company-domain.com`.
3. Sign in once to confirm the app loads.
4. Tap the browser menu.
5. Tap **Install app** or **Add to Home screen**.
6. Confirm the installation.

The Art Decor icon will appear on the Android home screen.

## iPhone

1. Open Safari on the iPhone.
2. Visit the app URL, for example `https://your-company-domain.com`.
3. Tap the **Share** button.
4. Tap **Add to Home Screen**.
5. Confirm the name **Art Decor**.
6. Tap **Add**.

The Art Decor icon will appear on the iPhone home screen.

## Offline Attendance Behavior

- The installed app shell opens even when the network is unavailable.
- Employee attendance actions are saved locally when offline.
- Queued actions automatically retry when the device returns online.
- If sync cannot complete, the pending count remains visible to the employee.

## Production Notes

- Use HTTPS in production. PWA installation, service workers, and mobile app behavior require a secure origin.
- Keep `VITE_API_BASE_URL` pointed at the production backend API.
- Keep `CORS_ALLOWED_ORIGINS` on the backend restricted to the production frontend domain.
