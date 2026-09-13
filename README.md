# Handpicked GPS Camera

Starter Android project for a GPS camera app.

## Current MVP foundation
- CameraX back-camera preview
- Runtime camera + location permissions
- Fused Location Provider
- Live GPS coordinate/accuracy overlay foundation
- Handpicked GPS Camera branding
- Architecture ready for photo stamping, Firebase backend, Remote Config, Ads and Premium controls

## Next implementation modules
1. Capture photo and permanently burn GPS/date/time/address watermark into the image.
2. Reverse-geocode coordinates to address.
3. Firebase Authentication + Firestore/Remote Config.
4. Admin web dashboard for users, device access, stamp templates, ads and feature flags.
5. AdMob integration behind a remote `ads_enabled` flag.
6. Premium entitlement and subscription verification.

Add your Firebase `google-services.json` only after creating your own Firebase project; it is intentionally not included.
