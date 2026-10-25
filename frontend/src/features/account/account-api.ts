import type { LoginResponse, UserProfile } from "@/lib/api/contracts";
import { apiRequest } from "@/lib/api/http";

export type UpdateOwnProfileInput = {
  firstName: string;
  lastName: string;
  email: string;
  phone: string | null;
  birthDate: string | null;
};

export function getOwnProfile() {
  return apiRequest<UserProfile>("/api/v1/users/me", { authenticated: true });
}

export function updateOwnProfile(input: UpdateOwnProfileInput) {
  return apiRequest<UserProfile>("/api/v1/users/me", {
    method: "PUT",
    authenticated: true,
    body: input,
  });
}

export function changeOwnPassword(input: { currentPassword: string; newPassword: string }) {
  return apiRequest<LoginResponse>("/api/v1/users/me/password", {
    method: "POST",
    authenticated: true,
    body: input,
  });
}
