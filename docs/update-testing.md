# Local update testing

This guide covers two separate manual test paths:

- the **GitHub flavour**, using a local HTTP release feed and controlled APKs;
- the **Play flavour**, using real Google Play internal app sharing.

The scripts do not publish artefacts or perform device, emulator or Play Console tests. The expected results below are a test plan, not a record that those external steps have been performed.

## 1. Prepare the GitHub update-test APKs

From the repository root, run:

```bash
./scripts/prepare_update_test_apks.sh
```

The script runs `assemblePlayDebug` with the `hydroVersionCode`, `hydroVersionName` and `hydroApplicationIdSuffix` Gradle properties. It reads the current `configuredVersionName` and `configuredVersionCode` from `app/build.gradle.kts` as the installed baseline, then creates a synthetic update with the next integer version code and one additional numeric version-name component (for example, 1.8 → 1.8.1). It does not alter the project's signing configuration or assume the next public release name. The normal APKs therefore use the same signing configuration as an ordinary local `playDebug` build. It also writes `build/update-test/test-config.json`, which the server uses instead of hardcoded version constants.

Generated files are placed under `build/update-test/`:

| File | Purpose |
| --- | --- |
| `HydroTracker-<update-version>.apk` | Valid update: the synthetic update version and code, application ID `com.cemcakmak.hydrotracker.debug`. |
| `HydroTracker-<baseline-version>.apk` | Baseline installation and deliberately old update: the current project version and code, normal debug application ID and signature. |
| `HydroTracker-<update-version>-wrong-package.apk` | The update version with application ID `com.cemcakmak.hydrotracker.wrong`. |
| `HydroTracker-<update-version>-bad-signature.apk` | A copy of the valid update re-signed with a different, disposable test key. |
| `bad-signature-test.keystore` | Test-only key used for the bad-signature APK; never use it for distribution. |

The build requires Python 3, a JDK with `keytool` and Android SDK build-tools containing `apksigner`. The script searches `PATH`, `ANDROID_HOME`, `ANDROID_SDK_ROOT` and the usual macOS SDK location. Project version names must use dotted numeric components so the synthetic update always compares as newer.

Install the generated baseline APK before testing an update (substitute the filename printed by the script):

```bash
adb install -r build/update-test/HydroTracker-<baseline-version>.apk
```

The debug version name visible in Android may include the normal `-debug` suffix. This does not change the numeric update comparison. **playDebug is the only debug variant** and contains Play, GitHub and F-Droid-simulated behaviour. Open **Settings → Developer Options → Update testing** and select **GitHub** as the runtime channel for the scenarios in this guide. Play and F-Droid UI simulation use the same screen; no other debug APK is needed.

## 2. Start the local release server

Run:

```bash
python3 scripts/update_test_server.py
```

The server uses only the Python standard library. Its defaults are:

- artefact root: `build/update-test`;
- listen address: `0.0.0.0`;
- port: `8765`;
- slow-download delay: 0.05 seconds per 16 KiB chunk.

Startup output lists the available feed URLs and every scenario. For example, the host-machine success feed is:

```text
http://127.0.0.1:8765/release.json?scenario=success
```

Optional arguments are available for a different directory, interface, port or throttle:

```bash
python3 scripts/update_test_server.py \
  --root build/update-test \
  --host 0.0.0.0 \
  --port 8765 \
  --slow-delay 0.1
```

The server reads APK sizes and SHA-256 digests at startup. Stop and restart it after regenerating the APKs. It serves only the release feed, a plain-text index and the four known APKs; it does not expose the keystore or provide a directory listing. Because it uses unencrypted HTTP and binds to all interfaces by default, run it only on a trusted test network and stop it when finished.

## 3. Connect a test device

Configure the **debug-only local update feed override** in the GitHub test build to the required feed URL. Do not replace the production GitHub endpoint. The local test build must permit cleartext HTTP only for this debug harness.

**Android 17 also requires the `ACCESS_LOCAL_NETWORK` runtime permission.** The first server check requests it in the Update Testing screen. Grant it when prompted; it is required only for the local test server, not for production GitHub releases. To grant it manually, use:

```bash
adb -s <device-serial> shell pm grant com.cemcakmak.hydrotracker.debug android.permission.ACCESS_LOCAL_NETWORK
adb -s <device-serial> shell am force-stop com.cemcakmak.hydrotracker.debug
```

Reopen HydroTracker after granting it manually. Setting only the app-op does not grant the runtime permission.

### Android emulator

Use the emulator's special host-loopback address:

```text
http://10.0.2.2:8765/release.json?scenario=success
```

`10.0.2.2` refers to the computer running the server, not to the emulator itself.

### Physical device

Use the LAN address printed by the server:

```text
http://<computer-LAN-IP>:8765/release.json?scenario=success
```

The phone and computer must be on the same reachable network. Allow inbound TCP connections to port 8765 through the computer's firewall, and check that the Wi-Fi network does not isolate clients.

Each release response builds its `browser_download_url` from the request's `Host` header. Consequently, an emulator feed produces emulator-reachable APK URLs and a LAN feed produces LAN APK URLs without rewriting scenario files.

In the app, open **Settings → Developer Options → Update testing**. Under **Local server**, enter only the base URL (for example `http://10.0.2.2:8765`), cycle to the required scenario and tap **Save and check for updates**. The debug-only configuration is persisted between launches. The first installation attempt does not request install permission until the APK has downloaded and been verified. Tap **Install update** when it is ready; Android may then ask for **Install unknown apps** permission for HydroTracker. Allow it for the debug app and return to the app to resume installation automatically. If permission is denied, **Allow installation** remains available as a retry without downloading again.

## 4. GitHub scenario expectations

All installation scenarios first require the release check to show the generated update version as available. Start the update through the **normal Home update modal** (or **Download update** on the Updates screen), which then remains responsible for download progress, verification, installation and retry. Repeated taps on the normal action verify that duplicate starts cannot start concurrent installers.

| Scenario | Feed behaviour | Expected app result |
| --- | --- | --- |
| `success` | Tag and exact asset name derived from the generated update version; actual byte size and `sha256:<actual digest>`; valid newer APK. | Download reaches 100%, size/digest and package checks pass, and Android shows the system installation confirmation. Confirming installs the synthetic update; cancelling does not. |
| `up_to_date` | Tag derived from the installed baseline version. | **You're on the latest version**; no update download is offered. |
| `http_error` | Feed returns HTTP 500. | Update check fails with the HTTP 500 error; no APK is downloaded. |
| `invalid_json` | Feed returns HTTP 200 with malformed JSON. | Update check fails with a JSON parsing error; no APK is downloaded. |
| `missing_asset` | Newer release contains only the non-matching `HydroTracker-<update-version>-fdroid.apk` asset name. | Check fails because no exact GitHub update APK is available; the non-matching asset is not downloaded. |
| `bad_digest` | Valid APK and size, but digest is `sha256:` followed by 64 zeroes. | Download finishes, then fails the SHA-256 check. No package-installer confirmation appears. |
| `wrong_package` | Valid metadata for the APK whose application ID ends in `.wrong`. | Download and digest checks finish, then installation is rejected for the wrong application ID. |
| `old_version` | Release advertises the synthetic update but serves the baseline APK. | Download and digest checks finish, then installation is rejected because the APK is not newer than the installed baseline. |
| `bad_signature` | Correct package and higher code, signed by the disposable test key. | Download, package and version checks finish, then installation is rejected for an unexpected signing certificate. |
| `slow_download` | Valid success metadata; APK response is throttled in 16 KiB chunks. | Download progress remains visible long enough to exercise backgrounding, notifications and continuation. Installation otherwise behaves like `success`. |

For failure scenarios, verify that the old app remains installed and usable and that no system installation confirmation appears. Error wording can vary by Android or JSON parser version; the important result is the failed stage described above.

The same screen also provides debug-only state simulations for every update UI state. Its installer-result controls abandon the active system session before simulating success or failure, so they cannot leave an APK to be installed unexpectedly later.

## 5. Background, notification and Continue installation tests

Use `slow_download` for these tests so the download does not finish before the app is backgrounded. Do not force-stop the app during a pending-installation test.

### Background notification

1. Install the generated baseline and enable notifications for HydroTracker.
2. Select `slow_download`, check for updates and start the download while the app is foregrounded.
3. Press Home and leave the app in the background until the download and staging complete.
4. Expect the progress notification to be replaced by **Update ready to install**. Tapping it should open HydroTracker's Home screen with the update modal visible.
5. Tap **Install update** in the modal to reach Android's system confirmation, then confirm or cancel and verify the final installer result.

### Live Updates on Android 16 and later

1. On an Android 16+ device, open **Settings → Notifications → Live Updates**. If Android blocks promoted notifications, use **Allow** to open Android's Live Updates settings, then return and confirm that the in-app toggle is available.
2. Start `slow_download` and verify that determinate progress uses the cloud-download icon and percentage, while an indeterminate response uses the icon without a percentage.
3. Dismiss the Live Update during the same download. Subsequent progress updates must use the standard notification rather than repeatedly requesting promotion. A new download attempt resets this dismissal.
4. Disable the in-app Live Updates toggle and verify the standard progress notification returns without changing the download itself. Disable Live Updates in Android settings and verify that the in-app toggle becomes unavailable with a link back to system settings.
5. Complete the download and confirm that the ongoing Live Update is removed. The ready-to-install notification remains a standard notification.

### Cancelling a download

1. Start `slow_download`, then use **Cancel download** in both the regular and Live Update notifications and in the Home update modal. Verify that the connection stops, the ongoing notification is removed, partial files are deleted, and the same update returns to **Available** for retry. In the modal, **Cancel download** also dismisses the dialog without immediately reopening it.
2. Repeat cancellation while verification is in progress. Ready-to-install and Android's system installation confirmation are not cancellable through this action.
3. Use **Later** instead and verify that only the modal closes while the download continues in the background.
4. Pull to refresh on Home to check for updates. A new check may reopen the update modal; an already active update session is not checked redundantly and shows an **Update available** Toast with its version instead.

### Continue installation fallback

1. Disable HydroTracker's notifications, or deny its notification permission on Android 13 or later.
2. Repeat the `slow_download` flow and background the app before staging completes.
3. No confirmation notification should be available. Reopen HydroTracker and return to **Settings → Updates**.
4. The status should remain **Installing update…** with **Continue installation**. Tap it and verify that Android's pending confirmation opens.
5. Complete or cancel the confirmation and check the resulting app status.

Also run `success` once while remaining in the foreground with the modal visible. No ready notification is needed in that case: the modal itself presents **Install update**, which opens Android's system confirmation.

## 6. Reset after a successful installation

A successful `success` or `slow_download` test leaves the synthetic update installed. Restore the generated baseline before repeating a new-update scenario. Because the test packages are debuggable, try:

```bash
adb install -r -d build/update-test/HydroTracker-<baseline-version>.apk
```

If Android rejects the downgrade, use a clean baseline instead:

```bash
adb uninstall com.cemcakmak.hydrotracker.debug
adb install build/update-test/HydroTracker-<baseline-version>.apk
```

**The clean-baseline commands erase the debug app's local data.** After reinstalling, check that Updates reports the baseline version before starting another scenario.

## 7. Real Google Play in-app update test

This is independent of the local GitHub server. Use the Play flavour and real, correctly signed Play artefacts with the same application ID and signing identity. The old artefact must have a lower version code than the new artefact. Use Google Play's documented **internal app sharing** workflow:

1. Upload the **old** Play build to internal app sharing.
2. On the test device, open the old build's sharing link and install that build through Google Play.
3. Upload a **newer** build with a higher version code to internal app sharing.
4. Wait for the new upload to become available, then open the **newer build's sharing link on the device but do not install it**. Opening the link makes Google Play aware of the available newer version.
5. Open the already installed **old** app, navigate to **Settings → Updates** and check for updates.
6. Verify that Google Play offers an in-app update. Exercise the update type allowed by Play: for a flexible update, also verify the downloaded state and **Restart to install**; for an immediate update, verify Play's full-screen flow and the final relaunch.

If no update is offered, check upload propagation, version codes, application IDs, signing identities, the Play account's tester access and Play Store cache/state before treating it as an app failure. Internal app sharing requires real Play Console and device access; the local scripts neither upload these builds nor perform this test.

For an actual Google review submission, use the **internal test track** as required by the review workflow. Internal app sharing is for testing and is not a substitute for the internal-test-track review submission.
