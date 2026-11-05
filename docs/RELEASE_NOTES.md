# FitQuest release notes

## Version 2.0.0 (POE final release)

This release adds everything since the Part 2 prototype (version 1.0).

### Innovative features added in this version

These are the features I specified during Part 1 and completed for the POE.

| Feature | What changed |
|---|---|
| Badges (user-defined feature 4) | New trophy cabinet showing every badge, with progress towards locked ones. Badges are localised and trigger a push notification. |
| Suggested workouts (user-defined feature 5) | Suggestions now rank muscle groups by rest time, show the focus areas and explain why they were chosen. The last suggestion stays available offline. |

### New required features

- **Biometric authentication:** fingerprint, face or device PIN after SSO, on launch, and after five minutes in the background. Turning the lock off requires verification.
- **Offline mode:** full workouts now sync, including exercises, sets and completion. Sync retries through WorkManager after the connection returns, even if the app was closed. An offline banner shows the current state, and the exercise library is cached.
- **Real-time notifications:** FCM push for badges and level-ups, a test button in Settings, and a local streak reminder at the chosen time.
- **Multi-language:** all interface text moved to string resources, with English, Afrikaans and isiZulu available in the app.
- **Blob storage:** profile pictures and progress photos in private Azure Blob Storage.
- **Logging:** Android `Log` calls throughout the app, covering lifecycle, sign-in, sync and notifications.

### Fixes from Part 2 feedback

| Lecturer feedback | Fix |
|---|---|
| GitHub state never checked | State is stored securely and compared when the app reopens. A mismatch rejects the code. |
| Expired token leaves the user looking signed in | A 401 ends the session and returns to sign-in. Unsynced workouts stay on the phone. |
| Units saved but always shown in kg | Input and all displays now convert. Storage stays in kg. |
| Account card never shows who is signed in | Shows the name, email and profile picture. |
| Completing a workout twice awards XP again | Completion can only succeed once. |
| Muscle group split always empty | The API now calculates it from every set. |
| Progress endpoint never called | The Progress screen now uses it. |
| Personal records only checked on the phone | The API decides against full history and its answer replaces the local estimate. |
| Streak capped at 10 and level worked out on the phone | Both now come from the API. |
| Progress empty after reinstalling | History is restored from the API after sign-in. |
| Generic Material 3 design | New visual identity: brand mark, gradient hero card, gold level hexagon, ranks and medal cabinet. |
| File upload used for GitHub | Repository restructured and developed through commits, with working CI. |

### Other improvements

- Set input now has large steppers, a "same as last" shortcut, a skippable rest timer and validation.
- A workout cannot be finished with no sets, and the flow is forward-only, as designed in Part 1.
- The streak grace rule now uses calendar days, and late-synced workouts cannot break a streak.
- The API validates input and returns 400 or 404 instead of a server error.
- Release builds are signed and minified, and do not log network traffic.

### Refinements after code review

- Unfinished workouts can be resumed from Home, and the biometric relock no longer closes a workout in progress.
- Mistaken sets can be deleted during a workout, on the phone and on the server.
- Units, theme and other settings now work offline and upload on reconnect.
- Progress and Badges refresh each time they open.
- The workout total no longer stops at 50.
- Signing in with a second provider on the same email now explains the problem instead of failing.
- Rest ending now gives haptic feedback, and the exercise picker can be cancelled.

### Known limitations

- Exercise names come from the shared library and stay in English.
- Photos need a connection. They are not queued offline.
- Streak days are based on UTC.
- If a sign-out happens with workouts that could not be uploaded, they stay on the phone and upload under the next account that signs in.
