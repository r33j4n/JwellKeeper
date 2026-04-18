import { EncryptJWT, jwtDecrypt } from "jose";
import type { AuthUser } from "@/lib/api/types";

const encoder = new TextEncoder();
const SESSION_DURATION_SECONDS = 60 * 60 * 24;

async function key() {
  const secret = process.env.SESSION_SECRET || "local-development-session-secret-change-me";
  const digest = await crypto.subtle.digest("SHA-256", encoder.encode(secret));
  return new Uint8Array(digest);
}

export async function encryptSession(user: AuthUser) {
  const now = Math.floor(Date.now() / 1000);
  return new EncryptJWT({ ...user })
    .setProtectedHeader({ alg: "dir", enc: "A256GCM" })
    .setIssuedAt(now)
    .setExpirationTime(now + SESSION_DURATION_SECONDS)
    .encrypt(await key());
}

export async function decryptSession(session?: string | null): Promise<AuthUser | null> {
  if (!session) return null;
  try {
    const result = await jwtDecrypt(session, await key());
    const payload = result.payload as unknown as AuthUser;
    if (!payload.userId || !payload.tenantId || !payload.role) return null;
    return payload;
  } catch {
    return null;
  }
}

export const sessionCookieName = "jk_session";
export const tokenCookieName = "jk_token";
export const sessionMaxAge = SESSION_DURATION_SECONDS;
