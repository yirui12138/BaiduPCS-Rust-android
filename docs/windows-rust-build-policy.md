# Windows Rust Build Policy Block

This project's Android JNI Rust build can fail on some Windows 11 machines even when the project itself is correct.

## Root Cause

On this machine, Windows Smart App Control is enabled:

- `SmartAppControlState = On`
- `HKLM\SYSTEM\CurrentControlSet\Control\CI\Policy\VerifiedAndReputablePolicyState = 1`

When that policy is enforced, Cargo-generated host executables such as `build-script-build.exe` are treated as untrusted and blocked by Code Integrity before they can run.

That means project-local changes such as:

- moving Cargo target directories
- changing temporary directories
- rebuilding from a different folder under the same Windows installation

do **not** solve the problem.

## Typical Symptoms

- `cargo check` fails on crates that use build scripts
- Android Gradle Rust tasks fail before JNI `.so` files are rebuilt
- Code Integrity event log entries mention `build-script-build.exe`

## How To Verify

Run:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\check_windows_rust_build_policy.ps1
```

Or run the Gradle preflight:

```powershell
$env:JAVA_HOME='D:\android studio\jbr'
D:\实验\gradle-8.7\bin\gradle.bat :app:verifyWindowsRustBuildPolicy --no-daemon
```

If Smart App Control is the blocker, the task will fail early with a clear explanation instead of letting Cargo die later with a less readable error.

## Effective Ways To Unblock

1. Turn off Smart App Control in Windows Security, then reboot.
2. Build on another machine, VM, or WSL environment where local Rust build scripts are allowed to execute.

## Notes

- This is an operating-system policy issue, not a Rust crate bug in this repository.
- If you must keep Smart App Control enabled on the main machine, the practical workaround is to move Rust JNI builds to a different environment and copy the rebuilt `.so` files back into the Android packaging flow.
