import { afterEach } from "vitest";

Object.defineProperty(document, "compatMode", { value: "CSS1Compat" });

afterEach(() => {
  localStorage.clear();
  sessionStorage.clear();
});
