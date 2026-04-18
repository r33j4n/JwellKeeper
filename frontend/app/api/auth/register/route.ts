import { NextRequest, NextResponse } from "next/server";
import { cookies } from "next/headers";
import { encryptSession, sessionCookieName, sessionMaxAge, tokenCookieName } from "@/lib/session/session";
import type { ApiResponse, AuthResponse, AuthUser } from "@/lib/api/types";

const backendUrl = process.env.BACKEND_API_URL || "http://localhost:8080";

function cookieOptions() {
  return {
    httpOnly: true,
    sameSite: "lax" as const,
    secure: process.env.NODE_ENV === "production",
    path: "/",
    maxAge: sessionMaxAge,
  };
}

export async function POST(request: NextRequest) {
  const body = await request.text();
  const backendResponse = await fetch(`${backendUrl}/api/auth/register`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body,
  });

  const payload = (await backendResponse.json().catch(() => null)) as ApiResponse<AuthResponse> | null;
  if (!backendResponse.ok || !payload?.success || !payload.data?.token) {
    return NextResponse.json(payload || { success: false, message: "Registration failed", data: null }, { status: backendResponse.status || 500 });
  }

  const { token, ...user } = payload.data;
  const cookieStore = await cookies();
  cookieStore.set(tokenCookieName, token, cookieOptions());
  cookieStore.set(sessionCookieName, await encryptSession(user as AuthUser), cookieOptions());

  return NextResponse.json({ success: true, message: payload.message, data: user });
}
