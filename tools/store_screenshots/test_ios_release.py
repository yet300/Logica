import re
import unittest
from pathlib import Path


class IosReleaseFastfileTest(unittest.TestCase):
    def test_release_lane_builds_signed_ipa_and_uploads_only_to_testflight(self):
        fastfile = Path("fastlane/Fastfile").read_text(encoding="utf-8")
        lanes = re.findall(r"lane :release do(?P<body>.*?)\n  end", fastfile, re.DOTALL)
        self.assertGreaterEqual(len(lanes), 2)
        body = lanes[-1]
        for variable in (
            "APP_VERSION_NAME",
            "APP_VERSION_CODE",
            "APP_STORE_CONNECT_KEY_ID",
            "APP_STORE_CONNECT_ISSUER_ID",
            "APP_STORE_CONNECT_KEY_CONTENT",
            "IOS_PROVISIONING_PROFILE_NAME",
        ):
            self.assertIn(variable, body)
        for setting in (
            "build_app(",
            'project: "iosApp/iosApp.xcodeproj"',
            'scheme: "iosApp"',
            'configuration: "Release"',
            'export_method: "app-store"',
            'output_directory: "build/ios"',
            'output_name: "Logica.ipa"',
            '"ge.yet3.blokblast.BlockBlast"',
            '"3KKQ642Q9H"',
            '"CODE_SIGN_IDENTITY=\'Apple Distribution\'"',
            "upload_to_testflight(",
            "skip_waiting_for_build_processing: true",
            "distribute_external: false",
        ):
            self.assertIn(setting, body)
        self.assertNotIn("submit_for_review", body)
        self.assertNotIn("automatic_release", body)


class IosReleaseWorkflowTest(unittest.TestCase):
    def test_release_version_sources_are_200_with_build_15(self):
        gradle_properties = Path("gradle.properties").read_text(encoding="utf-8")
        config = Path("iosApp/Configuration/Config.xcconfig").read_text(encoding="utf-8")
        self.assertIn("appVersionName=2.0.0", gradle_properties)
        self.assertIn("appVersionCode=15", gradle_properties)
        self.assertIn("CURRENT_PROJECT_VERSION=15", config)
        self.assertIn("MARKETING_VERSION=2.0.0", config)

    def test_tag_workflow_builds_and_uploads_ios_artifacts_safely(self):
        workflow = Path(".github/workflows/release.yml").read_text(encoding="utf-8")
        self.assertRegex(workflow, r"(?m)^  ios:\s*$")
        self.assertIn("runs-on: macos-26", workflow)
        self.assertIn("needs: version", workflow)
        self.assertIn("APP_VERSION_NAME: ${{ needs.version.outputs.version_name }}", workflow)
        self.assertIn("APP_VERSION_CODE: ${{ needs.version.outputs.version_code }}", workflow)
        for secret in (
            "APP_STORE_CONNECT_KEY_ID",
            "APP_STORE_CONNECT_ISSUER_ID",
            "APP_STORE_CONNECT_KEY_CONTENT",
            "IOS_DIST_CERT_P12",
            "IOS_DIST_CERT_PASSWORD",
            "IOS_PROVISIONING_PROFILE",
            "IOS_PROVISIONING_PROFILE_NAME",
            "GOOGLE_SERVICE_INFO_PLIST",
        ):
            self.assertIn(f"secrets.{secret}", workflow)
        for behavior in (
            "security create-keychain",
            "openssl rand -hex 32",
            "security import",
            "security set-key-partition-list",
            "Provisioning Profiles",
            "iosApp/iosApp/GoogleService-Info.plist",
            "bundle exec fastlane ios release",
            "build/ios/Logica.ipa",
            "build/ios/Logica.app.dSYM.zip",
            "retention-days: 14",
            "if: always()",
            "security delete-keychain",
        ):
            self.assertIn(behavior, workflow)
        self.assertNotIn("submit_for_review", workflow)
        self.assertNotIn("secrets.IOS_KEYCHAIN_PASSWORD", workflow)

    def test_cleanup_paths_are_persisted_before_signing_material_is_installed(self):
        workflow = Path(".github/workflows/release.yml").read_text(encoding="utf-8")
        self.assertLess(
            workflow.index('echo "IOS_KEYCHAIN_PATH=$KEYCHAIN_PATH"'),
            workflow.index('security create-keychain'),
        )
        self.assertLess(
            workflow.index('echo "IOS_PROFILE_PATH=$PROFILE_DESTINATION"'),
            workflow.index('cp "$PROFILE_PATH" "$PROFILE_DESTINATION"'),
        )


if __name__ == "__main__":
    unittest.main()
