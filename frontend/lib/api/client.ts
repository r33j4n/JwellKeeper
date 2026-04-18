"use client";

import axios, { AxiosError } from "axios";
import { toast } from "sonner";
import { useAuthStore } from "@/lib/store/auth-store";
import type { ApiResponse } from "@/lib/api/types";

export const authClient = axios.create({
  baseURL: "/api/auth",
  headers: { "Content-Type": "application/json" },
});

export const apiClient = axios.create({
  baseURL: "/api/backend",
  headers: { "Content-Type": "application/json" },
});

function messageFromError(error: AxiosError<ApiResponse<unknown>>) {
  return error.response?.data?.message || error.message || "Something went wrong";
}

apiClient.interceptors.response.use(
  (response) => {
    const payload = response.data as ApiResponse<unknown>;
    if (payload && typeof payload === "object" && "success" in payload) {
      return { ...response, data: payload.data };
    }
    return response;
  },
  (error: AxiosError<ApiResponse<unknown>>) => {
    if (error.response?.status === 401) {
      useAuthStore.getState().clearUser();
      if (typeof window !== "undefined") {
        window.location.href = `/login?next=${encodeURIComponent(window.location.pathname)}`;
      }
      return Promise.reject(error);
    }
    toast.error(messageFromError(error));
    return Promise.reject(error);
  },
);

authClient.interceptors.response.use(
  (response) => {
    const payload = response.data as ApiResponse<unknown>;
    if (payload && typeof payload === "object" && "success" in payload) {
      return { ...response, data: payload.data };
    }
    return response;
  },
  (error: AxiosError<ApiResponse<unknown>>) => {
    toast.error(messageFromError(error));
    return Promise.reject(error);
  },
);
