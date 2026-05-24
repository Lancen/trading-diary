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
 * 用户名密码登录。
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
 * 使用 refresh token 刷新访问令牌。
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
 * 当前用户登出。
 * POST /api/v1/auth/logout
 */
export async function logout(): Promise<void> {
  await api.post("api/v1/auth/logout").json<ApiResponse<null>>();
}

/**
 * 获取当前认证用户信息。
 * GET /api/v1/auth/me
 */
export async function getMe(): Promise<UserInfoVO> {
  const res = await api
    .get("api/v1/auth/me")
    .json<ApiResponse<UserInfoVO>>();
  return res.data;
}
