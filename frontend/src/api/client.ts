const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;

if (!API_BASE_URL) {
  // Fails fast and visibly rather than silently building requests like
  // "undefined/auth/register" (which look like a 404 against the
  // frontend itself, not an obviously missing-config error).
  throw new Error(
    "VITE_API_BASE_URL is not set. Create frontend/.env.local (copy .env.example) " +
    "and restart `npm run dev` — Vite only reads env files at server startup."
  );
}

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

async function throwIfError(response: Response): Promise<void> {
  if (response.ok) return;

  let body: ApiErrorBody | undefined;
  try {
    const text = await response.text();
    body = text ? JSON.parse(text) : undefined;
  } catch {
    body = undefined;
  }

  throw new ApiRequestError(
    body ?? {
      timestamp: new Date().toISOString(),
      status: response.status,
      code: "UNKNOWN_ERROR",
      message: "Something went wrong. Please try again.",
      details: [],
    }
  );
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
    await throwIfError(response);
    return undefined as T;
  }

  await throwIfError(response);
  const text = await response.text();
  return (text ? JSON.parse(text) : undefined) as T;
}

/**
 * Multipart file upload. Deliberately does NOT set a Content-Type
 * header — the browser sets "multipart/form-data; boundary=..."
 * itself based on the FormData body, and overriding it manually
 * breaks the boundary the server expects.
 */
export async function apiUpload<T>(path: string, formData: FormData, token: string | null): Promise<T> {
  const headers: Record<string, string> = {};
  if (token) {
    headers["Authorization"] = `Bearer ${token}`;
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    method: "POST",
    headers,
    body: formData,
  });

  await throwIfError(response);
  const text = await response.text();
  return (text ? JSON.parse(text) : undefined) as T;
}

/**
 * Downloads binary content (a document's bytes) as a Blob, along with
 * the filename the server suggests via Content-Disposition.
 */
export async function apiDownload(
  path: string,
  token: string | null
): Promise<{ blob: Blob; filename: string | null }> {
  const headers: Record<string, string> = {};
  if (token) {
    headers["Authorization"] = `Bearer ${token}`;
  }

  const response = await fetch(`${API_BASE_URL}${path}`, { headers });
  await throwIfError(response);

  const disposition = response.headers.get("Content-Disposition") ?? "";
  const match = /filename\*?=(?:UTF-8'')?"?([^";]+)"?/i.exec(disposition);
  const filename = match ? decodeURIComponent(match[1]) : null;

  const blob = await response.blob();
  return { blob, filename };
}
