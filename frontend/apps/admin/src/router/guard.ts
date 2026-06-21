import type { AdminRole } from "@/features/auth/model";

export function resolveGuardTarget(
  toPath: string,
  role: AdminRole | null,
  allowedRoles?: AdminRole[]
): true | string {
  if (toPath === "/login") return role ? "/dashboard" : true;
  if (!role) return "/login";
  if (allowedRoles && !allowedRoles.includes(role)) return "/error/403";
  return true;
}
