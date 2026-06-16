import { NextRequest, NextResponse } from "next/server";
import { decryptSession, sessionCookieName } from "@/lib/session/session";

const ownerOnlyRoutes = ["/dashboard/archived-jewellery", "/dashboard/settings/shop", "/dashboard/settings/staff", "/dashboard/settings/billing"];
const authRoutes = ["/login", "/register"];

export default async function proxy(request: NextRequest) {
  const path = request.nextUrl.pathname;
  const session = await decryptSession(request.cookies.get(sessionCookieName)?.value);
  const isAuthenticated = Boolean(session);

  if (path === "/") {
    if (isAuthenticated) {
      return NextResponse.redirect(new URL("/dashboard", request.url));
    }

    return NextResponse.next();
  }

  if (authRoutes.includes(path) && isAuthenticated) {
    return NextResponse.redirect(new URL("/dashboard", request.url));
  }

  if (path.startsWith("/dashboard") && !isAuthenticated) {
    const loginUrl = new URL("/login", request.url);
    loginUrl.searchParams.set("next", path);
    loginUrl.searchParams.set("reason", "auth-required");
    return NextResponse.redirect(loginUrl);
  }

  if (ownerOnlyRoutes.some((route) => path.startsWith(route)) && session?.role !== "OWNER") {
    const dashboardUrl = new URL("/dashboard", request.url);
    dashboardUrl.searchParams.set("accessDenied", "owner");
    return NextResponse.redirect(dashboardUrl);
  }

  if (path.startsWith("/dashboard/billing") && session?.role === "STOCK_KEEPER") {
    const dashboardUrl = new URL("/dashboard", request.url);
    dashboardUrl.searchParams.set("accessDenied", "billing");
    return NextResponse.redirect(dashboardUrl);
  }

  return NextResponse.next();
}

export const config = {
  matcher: ["/", "/login", "/register", "/dashboard/:path*"],
};
