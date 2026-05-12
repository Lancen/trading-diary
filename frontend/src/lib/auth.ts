import api from "./api";

export interface TokenVO {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
}

export interface UserInfoVO {
  id: number;
  username: string;
  nickname: string;
  roles: string[];
}

interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
  timestamp: string;
}

/**
 * Login with username and password.
 * POST /api/v1/auth/login
 */
export async function login(username: string, password: string): Promise<TokenVO> {
  const res = await api
    .post("api/v1/auth/login", {
      json: { username, password },
    })
    .json<ApiResponse<TokenVO>>();
  return res.data;
}

/**
 * Refresh access token using refresh token.
 * POST /api/v1/auth/refresh
 */
export async function refreshToken(refreshTokenValue: string): Promise<TokenVO> {
  const res = await api
    .post("api/v1/auth/refresh", {
      json: { refreshToken: refreshTokenValue },
    })
    .json<ApiResponse<TokenVO>>();
  return res.data;
}

/**
 * Logout current user.
 * POST /api/v1/auth/logout
 */
export async function logout(): Promise<void> {
  await api.post("api/v1/auth/logout").json<ApiResponse<null>>();
}

/**
 * Get current authenticated user info.
 * GET /api/v1/auth/me
 */
export async function getMe(): Promise<UserInfoVO> {
  const res = await api
    .get("api/v1/auth/me")
    .json<ApiResponse<UserInfoVO>>();
  return res.data;
}
