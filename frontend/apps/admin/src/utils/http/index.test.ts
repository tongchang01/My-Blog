import MockAdapter from "axios-mock-adapter";
import { describe, expect, it, vi } from "vitest";
import { ApiClientError } from "./error";
import { createHttpClient, type AuthRefreshCoordinator } from "./index";

function createCoordinator(
  refresh: () => Promise<string>
): AuthRefreshCoordinator & { expire: ReturnType<typeof vi.fn> } {
  return {
    getAccessToken: () => "expired-access-token",
    refresh,
    expire: vi.fn()
  };
}

describe("HTTP session refresh", () => {
  it("shares one refresh operation between concurrent expired requests", async () => {
    let resolveRefresh!: (token: string) => void;
    const refresh = vi.fn(
      () =>
        new Promise<string>(resolve => {
          resolveRefresh = resolve;
        })
    );
    const coordinator = createCoordinator(refresh);
    const client = createHttpClient({ coordinator });
    const mock = new MockAdapter(client.instance);
    let requestCount = 0;

    mock.onGet(/\/protected\/[12]/).reply(config => {
      requestCount += 1;
      if (config.headers?.Authorization === "Bearer renewed-access-token") {
        return [200, { code: "00000", msg: "", data: config.url }];
      }
      return [401, { code: "10002", msg: "expired", data: null }];
    });

    const first = client.get<string>("/protected/1");
    const second = client.get<string>("/protected/2");
    await vi.waitFor(() => expect(refresh).toHaveBeenCalledTimes(1));
    resolveRefresh("renewed-access-token");

    await expect(Promise.all([first, second])).resolves.toEqual([
      { code: "00000", msg: "", data: "/protected/1" },
      { code: "00000", msg: "", data: "/protected/2" }
    ]);
    expect(requestCount).toBe(4);
    expect(coordinator.expire).not.toHaveBeenCalled();
  });

  it("expires the session once when shared refresh fails", async () => {
    const coordinator = createCoordinator(() =>
      Promise.reject(new Error("refresh rejected"))
    );
    const refresh = vi.spyOn(coordinator, "refresh");
    const client = createHttpClient({ coordinator });
    const mock = new MockAdapter(client.instance);

    mock
      .onGet(/\/protected\/[12]/)
      .reply(401, { code: "10002", msg: "expired", data: null });

    const results = await Promise.allSettled([
      client.get("/protected/1"),
      client.get("/protected/2")
    ]);

    expect(refresh).toHaveBeenCalledTimes(1);
    expect(coordinator.expire).toHaveBeenCalledTimes(1);
    expect(results.every(result => result.status === "rejected")).toBe(true);
  });

  it("never refreshes a request explicitly excluded from auth refresh", async () => {
    const coordinator = createCoordinator(() =>
      Promise.resolve("renewed-access-token")
    );
    const refresh = vi.spyOn(coordinator, "refresh");
    const client = createHttpClient({ coordinator });
    const mock = new MockAdapter(client.instance);

    mock
      .onPost("/api/auth/refresh")
      .reply(401, { code: "10002", msg: "expired", data: null });

    await expect(
      client.post("/api/auth/refresh", undefined, { skipAuthRefresh: true })
    ).rejects.toBeInstanceOf(ApiClientError);
    expect(refresh).not.toHaveBeenCalled();
  });
});
