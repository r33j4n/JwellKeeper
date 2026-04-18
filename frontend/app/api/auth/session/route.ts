import { NextResponse } from "next/server";
import { cookies } from "next/headers";
import { decryptSession, sessionCookieName } from "@/lib/session/session";

export async function GET() {
  const session = (await cookies()).get(sessionCookieName)?.value;
  const user = await decryptSession(session);
  return NextResponse.json({ success: true, message: "Session fetched", data: user });
}
