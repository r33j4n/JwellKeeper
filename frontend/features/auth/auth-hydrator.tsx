"use client";

import { useEffect } from "react";
import { authApi } from "@/lib/api/queries";
import { useAuthStore } from "@/lib/store/auth-store";

export function AuthHydrator() {
  const setUser = useAuthStore((state) => state.setUser);

  useEffect(() => {
    let active = true;
    authApi
      .session()
      .then((user) => {
        if (active) setUser(user);
      })
      .catch(() => {
        if (active) setUser(null);
      });
    return () => {
      active = false;
    };
  }, [setUser]);

  return null;
}
