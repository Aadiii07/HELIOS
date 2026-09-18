const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;

export interface ApiErrorBody {
  timestamp: string;
  status: number;
  code: string;
  message: string;
  details: string[];
}

export class ApiRequestError extends Error {
  readonly status: number;
  readonly code: string;
  readonly details: string[];

  constructor(body: ApiErrorBody) {
    super(body.message);
    this.status = body.status;
    this.code = body.code;
    this.details = body.details ?? [];
  }
}

interface RequestOptions {
  method?: "GET" | "POST" | "PUT" | "DELETE";
  body?: unknown;
  token?: string | null;
}

export async function apiRequest<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const headers: Record<string, string> = {};
  if (options.body !== undefined) {
    headers["Content-Type"] = "application/json";
  }
  if (options.token) {
    headers["Authorization"] = `Bearer ${options.token}`;
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    method: options.method ?? "GET",
    headers,
    body: options.body !== undefined ? JSON.stringify(options.body) : undefined,
  });

  if (response.status === 204) {
    return undefined as T;
  }

  const text = await response.text();
  const data = text ? JSON.parse(text) : undefined;

  if (!response.ok) {
    throw new ApiRequestError(
      data ?? {
        timestamp: new Date().toISOString(),
        status: response.status,
        code: "UNKNOWN_ERROR",
        message: "Something went wrong. Please try again.",
        details: [],
      }
    );
  }

  return data as T;
}
