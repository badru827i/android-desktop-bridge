# Desktop input protocol

Input is intentionally separate from the video stream.

Frames use: `magic=ADBI`, `version=1`, `type`, `sequence`, `timestamp_us`, `payload_len`, `payload`.

Types:
- `1` mouse move
- `2` mouse button
- `3` mouse wheel
- `4` key down
- `5` key up
- `6` touch

Security rules:
- accept input only from the authenticated paired peer;
- reject stale/replayed sequence numbers;
- cap payload size and rate;
- never execute input payload as code;
- stop accepting control events when the desktop session is closed.

Android Accessibility/input-control APIs require explicit user consent where applicable. Basic video capture does not silently grant control of other apps.
