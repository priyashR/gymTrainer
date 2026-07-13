import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { renderHook, act } from "@testing-library/react";
import { useWakeLock } from "../useWakeLock";

describe("useWakeLock", () => {
  const mockRelease = vi.fn().mockResolvedValue(undefined);
  const mockSentinel = { release: mockRelease } as unknown as WakeLockSentinel;

  beforeEach(() => {
    vi.resetAllMocks();
  });

  afterEach(() => {
    // Clean up navigator.wakeLock mock
    if ("wakeLock" in navigator) {
      Object.defineProperty(navigator, "wakeLock", {
        value: undefined,
        writable: true,
        configurable: true,
      });
    }
  });

  it("acquires a screen wake lock when API is supported", async () => {
    const mockRequest = vi.fn().mockResolvedValue(mockSentinel);
    Object.defineProperty(navigator, "wakeLock", {
      value: { request: mockRequest },
      writable: true,
      configurable: true,
    });

    const { result } = renderHook(() => useWakeLock());

    await act(async () => {
      await result.current.acquire();
    });

    expect(mockRequest).toHaveBeenCalledWith("screen");
  });

  it("releases the wake lock sentinel when release is called", async () => {
    const mockRequest = vi.fn().mockResolvedValue(mockSentinel);
    Object.defineProperty(navigator, "wakeLock", {
      value: { request: mockRequest },
      writable: true,
      configurable: true,
    });

    const { result } = renderHook(() => useWakeLock());

    await act(async () => {
      await result.current.acquire();
    });

    await act(async () => {
      await result.current.release();
    });

    expect(mockRelease).toHaveBeenCalledOnce();
  });

  it("does not throw when Wake Lock API is not supported", async () => {
    // Ensure wakeLock is not on navigator
    Object.defineProperty(navigator, "wakeLock", {
      value: undefined,
      writable: true,
      configurable: true,
    });

    const { result } = renderHook(() => useWakeLock());

    // Should not throw
    await act(async () => {
      await result.current.acquire();
    });

    await act(async () => {
      await result.current.release();
    });
  });

  it("silently handles errors when wake lock request fails", async () => {
    const mockRequest = vi.fn().mockRejectedValue(new Error("Not allowed"));
    Object.defineProperty(navigator, "wakeLock", {
      value: { request: mockRequest },
      writable: true,
      configurable: true,
    });

    const { result } = renderHook(() => useWakeLock());

    // Should not throw even though request rejects
    await act(async () => {
      await result.current.acquire();
    });

    expect(mockRequest).toHaveBeenCalledWith("screen");
  });

  it("does nothing when release is called without a prior acquire", async () => {
    const { result } = renderHook(() => useWakeLock());

    // Should not throw when releasing without acquiring
    await act(async () => {
      await result.current.release();
    });

    expect(mockRelease).not.toHaveBeenCalled();
  });
});
