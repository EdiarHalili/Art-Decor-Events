# Final QA Checklist

Use this checklist during the final QA and bug-fix phase before deployment.

## Authentication

- Admin can log in with a valid account.
- Admin cannot log in with a wrong password.
- Employee can log in with employee ID and password.
- Inactive employee cannot log in.
- Session expires according to configured policy.
- Unauthorized API calls return clear errors.

## Daily Check-In Windows

- Admin can create a daily check-in window for a selected date.
- Default open/close times come from Admin Settings.
- Admin can select allowed employees.
- Admin can edit a scheduled window.
- Admin can manually open a window.
- Admin can manually close a window.
- Admin can cancel a window.
- Cancelled windows block check-in.
- Unassigned employees cannot check in.

## Check In And Check Out

- Employee sees only today’s assignment, status, actions, and announcements.
- Employee can check in only when the window is open.
- Duplicate check-in is blocked.
- Employee can check out after check-in.
- Duplicate check-out is blocked.
- Late status follows the configured allowed late minutes.
- Clear error messages are shown for closed, cancelled, duplicate, and unassigned cases.

## Offline Mode

- Employee can queue check-in while offline.
- Employee can queue check-out while offline.
- Queued records sync when internet returns.
- Sync failures remain queued.
- Employee sees pending offline count.

## Reports And Exports

- Daily report loads.
- Weekly report loads.
- Monthly report loads.
- Employee history loads from employee profile.
- Worked hours and overtime totals display correctly.
- CSV export downloads.
- Excel export downloads.
- PDF export downloads.

## PWA

- App installs on Android from Chrome.
- App installs on iPhone from Safari Add to Home Screen.
- Company logo appears as app icon.
- Splash screen branding appears.
- App opens in standalone mode.
- App shell loads after refresh.

## GPS

- GPS enabled setting allows coordinate capture.
- GPS disabled setting prevents coordinate capture.
- Attendance still works when user denies location permission.
- Stored records are ready for future geofencing checks.

## Notifications

- Admin can publish an announcement.
- Employee sees visible announcements.
- Browser push permission request works where supported.
- Push subscription is saved when VAPID public key is configured.
- Reminder delivery remains documented as production sender work.

## Settings

- Company name updates across UI after refresh.
- Logo URL updates login, admin, and employee headers.
- Brand colors update the UI.
- Timezone accepts valid zone IDs.
- Invalid colors and invalid time ranges show clear errors.
- GPS and notifications toggles persist.

## Security

- Rate limiting blocks repeated login/API attempts.
- Admin-only APIs reject employee sessions.
- Employee APIs reject admin sessions.
- Audit logs record login, check-in, check-out, employee, window, announcement, and settings actions.
- Production CORS allows only the deployed frontend.

## Mobile Responsiveness

- Employee screen works on small phones.
- Admin navigation works on mobile.
- Daily window employee selection is usable on mobile.
- Settings page fields fit without overlap.
- Employee profile/history fits without horizontal scrolling.
