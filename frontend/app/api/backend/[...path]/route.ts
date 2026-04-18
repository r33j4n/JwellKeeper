import { NextRequest, NextResponse } from "next/server";
import { cookies } from "next/headers";
import { tokenCookieName } from "@/lib/session/session";

const backendUrl = process.env.BACKEND_API_URL || "http://localhost:8080";

type Context = { params: Promise<{ path: string[] }> };

async function handler(request: NextRequest, context: Context) {
  const token = (await cookies()).get(tokenCookieName)?.value;
  if (!token) {
    return NextResponse.json({ success: false, message: "Unauthorized", data: null }, { status: 401 });
  }

  const { path } = await context.params;
  const target = new URL(`${backendUrl}/api/${path.join("/")}`);
  request.nextUrl.searchParams.forEach((value, key) => target.searchParams.set(key, value));

  const headers = new Headers(request.headers);
  headers.set("Authorization", `Bearer ${token}`);
  headers.delete("host");
  headers.delete("cookie");

  const hasBody = !["GET", "HEAD"].includes(request.method);
  const backendResponse = await fetch(target, {
    method: request.method,
    headers,
    body: hasBody ? await request.arrayBuffer() : undefined,
    redirect: "manual",
  });

  const responseHeaders = new Headers();
  const contentType = backendResponse.headers.get("content-type");
  const disposition = backendResponse.headers.get("content-disposition");
  if (contentType) responseHeaders.set("content-type", contentType);
  if (disposition) responseHeaders.set("content-disposition", disposition);

  return new NextResponse(backendResponse.body, {
    status: backendResponse.status,
    headers: responseHeaders,
  });
}

export const GET = handler;
export const POST = handler;
export const PUT = handler;
export const PATCH = handler;
export const DELETE = handler;
