#!/usr/bin/env python3
"""Unit tests for the local HydroTracker update-test server."""

from __future__ import annotations

import http.client
import json
import tempfile
import threading
import unittest
from pathlib import Path

from scripts.update_test_server import UpdateTestConfig, UpdateTestServer, load_artefacts


class UpdateTestServerTest(unittest.TestCase):
    def setUp(self) -> None:
        self.temp_dir = tempfile.TemporaryDirectory()
        root = Path(self.temp_dir.name)
        self.config = UpdateTestConfig(
            baseline_version_name="9.4",
            baseline_version_code=104,
            update_version_name="9.4.1",
            update_version_code=105,
            artefact_filenames={
                "valid": "HydroTracker-9.4.1.apk",
                "old": "HydroTracker-9.4.apk",
                "wrong_package": "HydroTracker-9.4.1-wrong-package.apk",
                "bad_signature": "HydroTracker-9.4.1-bad-signature.apk",
            },
        )
        manifest = {
            "baselineVersionName": self.config.baseline_version_name,
            "baselineVersionCode": self.config.baseline_version_code,
            "updateVersionName": self.config.update_version_name,
            "updateVersionCode": self.config.update_version_code,
            "artefacts": self.config.artefact_filenames,
        }
        (root / "test-config.json").write_text(json.dumps(manifest), encoding="utf-8")
        for index, filename in enumerate(self.config.artefact_filenames.values(), start=1):
            (root / filename).write_bytes(f"test-apk-{index}".encode("ascii"))
        self.server = UpdateTestServer(
            ("127.0.0.1", 0),
            load_artefacts(root, self.config),
            slow_delay=0,
            config=self.config,
        )
        self.thread = threading.Thread(target=self.server.serve_forever, daemon=True)
        self.thread.start()
        self.port = self.server.server_address[1]

    def tearDown(self) -> None:
        self.server.shutdown()
        self.server.server_close()
        self.thread.join(timeout=5)
        self.temp_dir.cleanup()

    def get(self, path: str) -> tuple[int, dict[str, str], bytes]:
        connection = http.client.HTTPConnection("127.0.0.1", self.port, timeout=5)
        connection.request("GET", path)
        response = connection.getresponse()
        body = response.read()
        headers = {name.lower(): value for name, value in response.getheaders()}
        connection.close()
        return response.status, headers, body

    def release(self, scenario: str) -> dict:
        status, _, body = self.get(f"/release.json?scenario={scenario}")
        self.assertEqual(200, status)
        return json.loads(body)

    def test_success_has_exact_asset_size_digest_and_dynamic_url(self) -> None:
        release = self.release("success")
        asset = release["assets"][0]
        artefact = self.server.artefacts["valid"]
        update_name = self.config.update_version_name

        self.assertEqual(f"v{update_name}", release["tag_name"])
        self.assertEqual(f"HydroTracker-{update_name}.apk", asset["name"])
        self.assertEqual(artefact.size, asset["size"])
        self.assertEqual(f"sha256:{artefact.sha256}", asset["digest"])
        self.assertEqual(
            f"http://127.0.0.1:{self.port}/apks/HydroTracker-{update_name}.apk",
            asset["browser_download_url"],
        )

    def test_bad_digest_and_missing_asset_scenarios(self) -> None:
        bad_digest = self.release("bad_digest")["assets"][0]
        self.assertEqual("sha256:" + "0" * 64, bad_digest["digest"])

        missing = self.release("missing_asset")["assets"][0]
        self.assertEqual(
            f"HydroTracker-{self.config.update_version_name}-fdroid.apk",
            missing["name"],
        )

    def test_up_to_date_http_error_and_invalid_json(self) -> None:
        self.assertEqual(
            f"v{self.config.baseline_version_name}",
            self.release("up_to_date")["tag_name"],
        )

        status, _, _ = self.get("/release.json?scenario=http_error")
        self.assertEqual(500, status)

        status, _, body = self.get("/release.json?scenario=invalid_json")
        self.assertEqual(200, status)
        with self.assertRaises(json.JSONDecodeError):
            json.loads(body)

    def test_apk_serving_is_restricted_to_known_files(self) -> None:
        status, headers, body = self.get(
            f"/apks/HydroTracker-{self.config.update_version_name}.apk"
        )
        self.assertEqual(200, status)
        self.assertEqual(self.server.artefacts["valid"].size, len(body))
        self.assertEqual(str(len(body)), headers["content-length"])

        status, _, _ = self.get("/apks/bad-signature-test.keystore")
        self.assertEqual(404, status)

    def test_unknown_scenario_is_rejected(self) -> None:
        status, _, _ = self.get("/release.json?scenario=not_a_scenario")
        self.assertEqual(400, status)


if __name__ == "__main__":
    unittest.main()
