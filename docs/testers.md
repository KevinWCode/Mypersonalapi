# Testing Personal API Rework

Thanks for trying it. Personal API Rework is an Android app that turns your day into a role-playing character.
Sleep, steps, workouts, screen time and what you log become XP, levels, stats and four vital bars (HP, Stamina, Mana
and Focus).

**Your data stays on your phone.** There's no account and no server, and nothing is sent anywhere unless you choose
to. Backups are files you save yourself.

## What you need

- An Android phone running Android 10 or newer. iPhone isn't supported.
- Optional: a watch or fitness app that writes to **Health Connect** (see below). Without one you still get screen
  time, food and the things you log by hand.

## 1. Install

1. On your phone, open
   [github.com/KevinWCode/Mypersonalapi/releases/tag/rework-latest](https://github.com/KevinWCode/Mypersonalapi/releases/tag/rework-latest)
   and download **personal-api-rework.apk**.
2. Open the downloaded file. Android will ask you to allow installs from your browser or Files app. Allow it, then
   tap **Install**.
3. If Play Protect warns that the app is unknown, tap **More details**, then **Install anyway**. It isn't on the Play
   Store yet, so Play Protect doesn't know it.

To update later, download the new APK the same way and install it over the old one. Your data stays.

## 2. First launch

The app opens on its Settings screen the first time. Work down it:

1. **Your activities**
   - **Your training:** what you train (e.g. Gym, Spin class, Running), your sessions each week (e.g.
     Monday, Wednesday, Friday) and optionally your exercises.
   - **Daily targets:** calories, protein and water.
   - **Social media target:** Contain, or a 5, 10 or 15% cut for bonus XP.
   - **Something you'd like to improve**, and optionally your own rewards.

   Tap **Save my activities**.
2. **Allow Health Connect access**: sleep, steps, heart rate and workouts.
3. **Allow usage access**: lets the app count screen time and social media minutes. Find "Personal API Rework" in
   the list and switch it on.
4. **Keep syncing in the background**: stops the phone pausing the app's 15-minute sync.

Then tap **Back to the dashboard**. Anything left empty (like training or a goal) just stays out of your quests, so
it never counts against you.

## 3. Connect your watch or fitness app (Health Connect)

The app reads from Health Connect, Android's shared health store. It's built into Android 14 and newer. On
Android 13 or older, install **Health Connect** from the Play Store first. Your fitness app has to write to it:

| App | Where to switch it on |
| --- | --- |
| Samsung Health | Samsung Health > ⋮ > Settings > Health Connect |
| Fitbit | Fitbit app > Today > your profile > Health Connect |
| Pixel Watch / Google Fit | Fitbit or Fit app > Profile > Settings > Health Connect |
| Garmin Connect | Garmin Connect > More > Settings > Connected apps > Health Connect |
| Oura | Oura > Settings > Health Connect |
| Whoop | Whoop > More > App settings > Integrations > Health Connect |
| Withings, Polar, Mi Fitness, Strava | In the app's settings, look for Health Connect or Connected apps |

Menu names change between versions. If you can't find it, open **Health Connect** in your phone's settings
(Settings > Security and privacy > Privacy > Health Connect, or search "Health Connect") and check that your app is
allowed to write.

If two apps write (say a Fitbit and the phone's own step counter), pick your main one under **Settings > Main
health app** so sleep and workouts aren't counted twice.

Not supported: Huawei Health doesn't share with Health Connect, and Apple Health is iPhone only.

## What gets tracked

- **Automatically:**
  - Sleep, steps, resting heart rate, HRV and workouts from your health app
  - Screen time, social media (TikTok, Instagram, Facebook, Reddit), YouTube, and calls and messaging
- **One tap on Daily quests:** water, coffee, reading, study, a journal entry and your workout.
- **Meal tiles:** food in two taps, or paste a meal from a food AI.
- **Optional:** GitHub commits and Alpaca trading accounts. You can switch both off in Your activities.

## Giving feedback

Please tell Kevin:

- Anything confusing on the first-launch screen
- Numbers that look wrong (for example, sleep counted twice or screen time far too high)
- Your phone make and model, and your health app
- Screenshots help a lot

To share your data for debugging, use **Settings > Back up phone data to a file** and send the file. Only do that if
you're happy to share it, because it contains everything the app has recorded.
