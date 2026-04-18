import { NextResponse } from "next/server";
import { cookies } from "next/headers";
import { sessionCookieName, tokenCookieName } from "@/lib/session/session";

export async function POST() {
  const cookieStore = await cookies();
  cookieStore.delete(tokenCookieName);
  cookieStore.delete(sessionCookieName);
  return NextResponse.json({ success: true, message: "Logged out", data: null });
}
