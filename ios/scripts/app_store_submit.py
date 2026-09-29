#!/usr/bin/env python3
"""Submits an uploaded NextSet build for App Review via the App Store Connect API.

Waits until App Store Connect has processed the build, attaches it to the app
version being prepared and submits that version. If App Store Connect is still
missing something (agreements, screenshots, age rating, App Privacy, ...), the
API rejects the submission and the reasons are printed.

    ASC_KEY_ID=… ASC_ISSUER_ID=… ASC_KEY_P8="$(cat AuthKey_….p8)" \
        ios/scripts/app_store_submit.py --version 1.0 --build 2

Needs PyJWT with cryptography and requests: pip install "pyjwt[crypto]" requests
"""

import argparse
import os
import sys
import time

import jwt
import requests

API = "https://api.appstoreconnect.apple.com/v1"
BUNDLE_ID = "com.mariokernich.nextset"


class AppStoreConnect:
    def __init__(self, key_id, issuer_id, private_key):
        self.key_id = key_id
        self.issuer_id = issuer_id
        self.private_key = private_key
        self.token = None
        self.token_expiry = 0

    def _authorization(self):
        # Tokens may live 20 minutes at most; renew a little earlier.
        now = int(time.time())
        if now > self.token_expiry - 60:
            self.token_expiry = now + 15 * 60
            self.token = jwt.encode(
                {"iss": self.issuer_id, "iat": now, "exp": self.token_expiry, "aud": "appstoreconnect-v1"},
                self.private_key,
                algorithm="ES256",
                headers={"kid": self.key_id, "typ": "JWT"},
            )
        return f"Bearer {self.token}"

    def call(self, method, path, allow=(), **kwargs):
        response = requests.request(
            method, API + path, headers={"Authorization": self._authorization()}, timeout=60, **kwargs
        )
        if response.status_code >= 400 and response.status_code not in allow:
            fail(f"{method} {path} failed with HTTP {response.status_code}", response)
        return response.json() if response.content else {}


def describe_errors(response):
    try:
        errors = response.json().get("errors", [])
    except ValueError:
        return [response.text]
    lines = []
    for error in errors:
        lines.append(f"{error.get('title', '')}: {error.get('detail', '')}".strip(": "))
        # Submission problems (missing screenshots, age rating, …) come as associated errors.
        for items in (error.get("meta", {}).get("associatedErrors") or {}).values():
            for item in items:
                lines.append("  - " + (item.get("detail") or item.get("title") or str(item)))
    return lines


def fail(message, response=None):
    print(f"::error::{message}", file=sys.stderr)
    if response is not None:
        for line in describe_errors(response):
            print(line, file=sys.stderr)
    sys.exit(1)


def wait_for_build(asc, app_id, version, build, timeout):
    deadline = time.time() + timeout
    while True:
        builds = asc.call(
            "GET",
            "/builds",
            params={"filter[app]": app_id, "filter[version]": build, "filter[preReleaseVersion.version]": version},
        )["data"]
        state = builds[0]["attributes"]["processingState"] if builds else "not in App Store Connect yet"
        if state == "VALID":
            return builds[0]["id"]
        if state in ("FAILED", "INVALID"):
            fail(f"Build {version} ({build}) is {state} in App Store Connect")
        if time.time() > deadline:
            fail(f"Build {version} ({build}) still {state} after {timeout // 60} minutes")
        print(f"Build {version} ({build}): {state}, checking again in a minute")
        time.sleep(60)


def main():
    parser = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    parser.add_argument("--version", required=True, help="marketing version, e.g. 1.0")
    parser.add_argument("--build", required=True, help="build number, e.g. 2")
    parser.add_argument("--timeout", type=int, default=60 * 60, help="seconds to wait for processing")
    args = parser.parse_args()

    missing = [name for name in ("ASC_KEY_ID", "ASC_ISSUER_ID", "ASC_KEY_P8") if not os.environ.get(name)]
    if missing:
        fail("Missing environment variables: " + ", ".join(missing))
    asc = AppStoreConnect(os.environ["ASC_KEY_ID"], os.environ["ASC_ISSUER_ID"], os.environ["ASC_KEY_P8"])

    apps = asc.call("GET", "/apps", params={"filter[bundleId]": BUNDLE_ID})["data"]
    if not apps:
        fail(f"No app with the bundle ID {BUNDLE_ID} in App Store Connect")
    app_id = apps[0]["id"]

    build_id = wait_for_build(asc, app_id, args.version, args.build, args.timeout)
    print(f"Build {args.version} ({args.build}) has processed")

    versions = asc.call(
        "GET",
        f"/apps/{app_id}/appStoreVersions",
        params={"filter[platform]": "IOS", "filter[versionString]": args.version},
    )["data"]
    if not versions:
        fail(f"App Store Connect has no iOS version {args.version} to submit")
    version_id = versions[0]["id"]
    asc.call(
        "PATCH",
        f"/appStoreVersions/{version_id}/relationships/build",
        json={"data": {"type": "builds", "id": build_id}},
    )
    print(f"Version {args.version} now uses build {args.build}")

    # Reuse a submission that is still being put together (e.g. one started on the website).
    drafts = asc.call(
        "GET",
        "/reviewSubmissions",
        params={"filter[app]": app_id, "filter[platform]": "IOS", "filter[state]": "READY_FOR_REVIEW"},
    )["data"]
    if drafts:
        submission_id = drafts[0]["id"]
        items = asc.call("GET", f"/reviewSubmissions/{submission_id}/items", params={"include": "appStoreVersion"})
        included = any(
            (item.get("relationships", {}).get("appStoreVersion", {}).get("data") or {}).get("id") == version_id
            for item in items["data"]
        )
    else:
        submission_id = asc.call(
            "POST",
            "/reviewSubmissions",
            json={
                "data": {
                    "type": "reviewSubmissions",
                    "attributes": {"platform": "IOS"},
                    "relationships": {"app": {"data": {"type": "apps", "id": app_id}}},
                }
            },
        )["data"]["id"]
        included = False

    if not included:
        asc.call(
            "POST",
            "/reviewSubmissionItems",
            json={
                "data": {
                    "type": "reviewSubmissionItems",
                    "relationships": {
                        "reviewSubmission": {"data": {"type": "reviewSubmissions", "id": submission_id}},
                        "appStoreVersion": {"data": {"type": "appStoreVersions", "id": version_id}},
                    },
                }
            },
        )

    asc.call(
        "PATCH",
        f"/reviewSubmissions/{submission_id}",
        json={"data": {"type": "reviewSubmissions", "id": submission_id, "attributes": {"submitted": True}}},
    )
    print(f"NextSet {args.version} ({args.build}) is submitted for App Review")


if __name__ == "__main__":
    main()
