import { createContext, ReactNode, useContext, useEffect, useMemo, useState } from "react";
import { api } from "../lib/api";
import type { UserMe } from "../types/api";

interface AuthContextValue {
  user: UserMe | null;
  loading: boolean;
  login: (email: string, password: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserMe | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const token = localStorage.getItem("flowstock.token");
    if (!token) {
      setLoading(false);
      return;
    }
    api.get<UserMe>("/auth/me")
      .then((response) => setUser(response.data))
      .catch(() => localStorage.removeItem("flowstock.token"))
      .finally(() => setLoading(false));
  }, []);

  async function login(email: string, password: string) {
    const response = await api.post<{ token: string; user: UserMe }>("/auth/login", { email, password });
    localStorage.setItem("flowstock.token", response.data.token);
    setUser(response.data.user);
  }

  function logout() {
    localStorage.removeItem("flowstock.token");
    setUser(null);
  }

  const value = useMemo(() => ({ user, loading, login, logout }), [user, loading]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth deve ser usado dentro de AuthProvider");
  }
  return context;
}
