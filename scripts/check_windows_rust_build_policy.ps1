param()

$ErrorActionPreference = 'Stop'
$policyPath = 'HKLM:\SYSTEM\CurrentControlSet\Control\CI\Policy'
$repoRoot = Split-Path -Parent $PSScriptRoot
$probeScript = Join-Path $repoRoot 'backend\tools\rustc_host_wrapper.py'

function Get-PolicyState {
    try {
        $policy = Get-ItemProperty -Path $policyPath -ErrorAction Stop
        return $policy.VerifiedAndReputablePolicyState
    } catch {
        return $null
    }
}

$state = Get-PolicyState

if ($null -eq $state) {
    Write-Host 'Code Integrity policy registry key not found. Smart App Control is probably not enabled on this machine.'
    exit 0
}

if ($state -ne 1) {
    Write-Host ("Result: VerifiedAndReputablePolicyState = {0}. No Smart App Control build-script block was detected." -f $state)
    exit 0
}

Write-Host 'Result: Smart App Control is currently ON.' -ForegroundColor Yellow

if (-not (Test-Path $probeScript)) {
    Write-Host 'Probe script not found, so only the registry state could be checked.' -ForegroundColor Yellow
    Write-Host 'If Android Rust builds fail with access-denied or Code Integrity errors, move the build to another machine / VM / WSL or turn off Smart App Control.'
    exit 2
}

& python $probeScript --probe-smart-app-control
$probeExit = $LASTEXITCODE

if ($probeExit -eq 2) {
    Write-Host ''
    Write-Host 'Effective ways to unblock:' -ForegroundColor Yellow
    Write-Host '1. Turn off Smart App Control in Windows Security, then reboot.'
    Write-Host '2. Build on another machine, VM, or WSL environment that does not block local build scripts.'
    exit 2
}

if ($probeExit -eq 0) {
    Write-Host ''
    Write-Host 'No effective Smart App Control block was reproduced by the Cargo build-script probe.' -ForegroundColor Green
    exit 0
}

Write-Host ''
Write-Host ("Probe finished with unexpected exit code {0}. Treating the result as inconclusive." -f $probeExit) -ForegroundColor Yellow
exit 0
