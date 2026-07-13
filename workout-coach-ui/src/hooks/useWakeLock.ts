import { useRef } from "react";

/**
 * Hook that manages the Screen Wake Lock API to prevent the device
 * from sleeping during active workout sessions (Theater Mode).
 *
 * Gracefully no-ops when the API is not supported by the browser.
 */
export function useWakeLock() {
  const wakeLockRef = useRef<WakeLockSentinel | null>(null);

  const acquire = async () => {
    if ("wakeLock" in navigator) {
      try {
        wakeLockRef.current = await navigator.wakeLock.request("screen");
      } catch {
        /* silently fail — browser may deny if tab not visible */
      }
    }
  };

  const release = async () => {
    if (wakeLockRef.current) {
      await wakeLockRef.current.release();
      wakeLockRef.current = null;
    }
  };

  return { acquire, release };
}
