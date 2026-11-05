# Play Store readiness

## Release checklist

- [x] Application ID: `com.fitquest.app`
- [x] Version: 2.0.0 (code 2)
- [x] Target SDK 35, minimum SDK 26
- [x] R8 minification and resource shrinking on release builds
- [x] Release signing configured outside Git
- [x] Network logging removed from release builds
- [x] Privacy policy written
- [x] App icon and feature graphic in `docs/store/`
- [ ] Signed release APK and app bundle built
- [ ] Phone screenshots added to `docs/store/screenshots/`
- [ ] Release SHA-1 added to Google Cloud and Firebase
- [ ] Privacy policy published at a public URL

## Store listing

**App name:** FitQuest: Gym Workout RPG

**Short description (80 characters maximum):**

Log strength workouts offline. Earn XP, level up, build streaks and unlock badges.

**Full description:**

Turn every gym session into progress you can see.

FitQuest is a strength training log with a role-playing progression system. Log your sets in seconds, even with no signal, then earn XP, climb the ranks from Novice to Legend, build your streak and unlock badges.

WHY FITQUEST
• Offline first: your workout saves on your phone and syncs when you reconnect
• Real progress: XP, levels, streaks and personal records from your full history
• Smart suggestions: workouts built around your most rested muscle groups
• Clear insights: weekly volume, muscle group split and personal records
• Progress photos: keep a private visual record of your journey

MADE FOR THE GYM
• Large buttons that work one-handed between sets
• Automatic rest timer
• Kilograms or pounds
• English, Afrikaans and isiZulu
• Fingerprint or face lock
• Streak reminders at a time you choose

Sign in with Google or GitHub. There is no password to remember.

**Category:** Health & Fitness

**Contact email:** add your email address

**Privacy policy URL:** add the public link to `docs/PRIVACY_POLICY.md`

## Data safety answers

| Data | Collected | Purpose |
|---|---|---|
| Name and email | Yes | Account management |
| Photos | Yes, only if the user uploads them | App functionality |
| Fitness activity | Yes | App functionality |
| Device token | Yes | Notifications |

Data is encrypted in transit and is not sold or shared with third parties for advertising. Users can request deletion through the contact email.

## Content rating

Complete the questionnaire in Play Console. The app has no violence, gambling or user-to-user content.

## Graphics

| Asset | Requirement | File |
|---|---|---|
| App icon | 512 × 512 PNG | `docs/store/icon-512.png` |
| Feature graphic | 1024 × 500 PNG | `docs/store/feature-graphic.png` |
| Phone screenshots | At least 2, 16:9 or 9:16 | `docs/store/screenshots/` |
