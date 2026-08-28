# mDNS announcements publish an unreachable interface address

## Tracking

- Issue: https://github.com/plainhub/plain-app/issues/299
- Repository: https://github.com/plainhub/plain-app
- Branch/commit tested: `upstream/main` at `43eebc1c3dc88328c27363c8dd01c12b6fbfd898`
- Pull request: https://github.com/plainhub/plain-app/pull/348

## Priority and scope

This issue was selected because it affects the primary connection path to PlainApp, has reports from multiple users, and remains reproducible on current upstream code. It is older and more broadly applicable than the other leading open bug candidates that require unavailable vendor-specific hardware or media samples.

The investigation covers PlainApp's Android mDNS host responder and Linux `.local` resolution. Note/Markdown-editor work and unrelated discovery or server lifecycle changes are explicitly out of scope.

## Reported behavior

Users report that the configured `*.local` PlainApp address does not resolve or connect, while access through the phone's numeric LAN address continues to work. The issue discussion includes failures on Linux and Windows and across Wi-Fi and hotspot configurations.

## Reproduction

### Environment

- PlainApp F-Droid debug build `3.3.10` (`661`)
- Clean `upstream/main` worktree at `43eebc1c3dc88328c27363c8dd01c12b6fbfd898`
- Pixel 9 Pro running GrapheneOS/Android 17 (API 37)
- Linux host using Avahi and `mdns4_minimal` name-service resolution
- Phone connected over Wi-Fi with an active VPN tunnel interface
- PlainApp HTTP service on port `8180`

Device identifiers, the configured hostname, and network addresses are intentionally omitted.

### Steps

1. Build and install the clean upstream F-Droid debug APK.
2. Start PlainApp's web server and mDNS responder while both the Wi-Fi and VPN tunnel interfaces are active.
3. Confirm that the numeric Wi-Fi URL responds over HTTP.
4. Resolve the configured `.local` hostname through the Linux system resolver and attempt the same HTTP request through that hostname.
5. Send a direct unicast-response-requested mDNS A query to the phone and inspect the returned A record.

### Actual result

- The numeric Wi-Fi URL returns HTTP `200`.
- The system resolver caches exactly one IPv4 address for the `.local` name: the phone's VPN tunnel address.
- The resolver does not return the phone's Wi-Fi address, and the `.local` HTTP request fails.
- A direct mDNS query reaches the phone and receives one A record containing the correct Wi-Fi address.

The responder is therefore alive and reachable on Wi-Fi, but its unsolicited announcement poisons the neighbor's cache with an address that is not reachable from that Wi-Fi segment.

### Expected result

An mDNS announcement sent on an interface must advertise the address reachable through that interface. A Wi-Fi listener should resolve the PlainApp hostname to the phone's Wi-Fi address, and the `.local` URL should reach the same server as the numeric Wi-Fi URL.

## Investigation findings

In the reproduced baseline, `candidateInterfaces()` accepted every active, non-loopback IPv4 interface except interfaces whose names matched the mobile-data list. This included a point-to-point VPN interface such as `tun0`.

The baseline `MdnsHostResponder.broadcastService()` then collected the addresses of all candidate interfaces into one announcement packet and sent that identical packet through every candidate interface. Consequently, the packet sent on Wi-Fi could advertise an address owned by an unrelated VPN interface.

The query-response path is interface-scoped: `respondToPacket()` answers with the local address on which the query arrived. This explains why the direct query returns the correct Wi-Fi address while the system resolver, which already accepted the gratuitous announcement, retains the unreachable tunnel address.

The following alternatives were ruled out during reproduction:

- HTTP server failure: the numeric Wi-Fi URL returned `200`.
- Missing Linux mDNS support: Avahi was active and the host was configured for `.local` resolution.
- Responder startup or receive failure: the direct query received an answer from the phone.
- A stale released build: the failure was reproduced from a clean current-upstream build.

## Root cause

The mDNS interface selection admits point-to-point/non-multicast tunnel interfaces, and the gratuitous announcement path combines addresses from all selected interfaces into a single response sent on every interface. This leaks an unreachable VPN address into the Wi-Fi mDNS scope and allows it to replace the usable Wi-Fi address in a resolver cache.

## Implemented correction

The Android interface enumerator now requires an interface to be active, non-loopback, non-point-to-point, multicast-capable, and outside the existing mobile-data name list. Capability reads are best-effort; an interface whose properties cannot be read is not admitted into discovery.

`MdnsHostResponder.broadcastService()` now builds a separate packet for each outgoing interface. Both service announcements and hostname-only announcements contain exactly one A record: the address belonging to that outgoing interface. A Wi-Fi announcement can therefore no longer publish a VPN or unrelated LAN address.

The iOS interface provider was not changed. Valid Android Wi-Fi/hotspot multicast interfaces remain eligible, while point-to-point tunnels and interfaces that cannot carry multicast are excluded.

## Regression coverage

- `MdnsIfaceSelectorTest` proves that an active multicast-capable Wi-Fi interface remains eligible and that point-to-point, non-multicast, and mobile-data interfaces are rejected.
- `MdnsAnnouncementTest` proves that service and hostname-only announcements contain only their outgoing interface address, even when the source service information also contains a tunnel address.
- The complete `shared-lib` Android host suite passes: 90 tests, 0 failures, 0 errors, 0 skipped.

## Validation

Baseline build completed successfully:

```text
PATH=/usr/bin:/bin:/usr/sbin:/sbin:/usr/local/bin:$PATH ./gradlew :app:assembleFdroidDebug
BUILD SUCCESSFUL
```

Implementation validation completed successfully:

```text
./gradlew :shared-lib:testAndroidHostTest
90 tests, 0 failures, 0 errors, 0 skipped

PATH=/usr/bin:/bin:/usr/sbin:/sbin:/usr/local/bin:$PATH ./gradlew :app:assembleFdroidDebug
BUILD SUCCESSFUL

PATH=/usr/bin:/bin:/usr/sbin:/sbin:/usr/local/bin:$PATH ./gradlew :app:assembleDebug
BUILD SUCCESSFUL
```

The fixed F-Droid debug APK was installed over the same debug-only package on the physical phone; the release package was not modified. With the same Wi-Fi and VPN interfaces active:

- Before: the resolver returned only the tunnel address; numeric HTTP returned `200`; `.local` HTTP failed.
- After: the resolver returned only the Wi-Fi address; both numeric and `.local` HTTP returned `200`.

The fixed APK was also installed on a dedicated Android 12/API 31 x86_64 emulator. The app cold-launched successfully, remained stable after backgrounding, and its forwarded HTTP endpoint returned `200` both before and after backgrounding. The mDNS responder started and announced on the emulator's eligible interfaces. Stock emulator NAT did not carry guest multicast to the host, so host-side `.local` resolution was not treated as a product pass or failure.

`git diff --check` also passes. GitHub registered no CI checks for pull request #348 at report-posting time.

## Remaining limitations

- The baseline failure has been reproduced on one physical Android device and Linux resolver topology.
- Android 12 runtime and direct-network compatibility were validated, but stock emulator NAT prevented end-to-end host-LAN multicast validation.
- Windows and phone-hotspot validation have not yet been performed.
- No GitHub CI checks were registered for pull request #348 at report-posting time.
