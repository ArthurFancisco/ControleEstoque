import { FormEvent, useState } from "react";
import { Navigate } from "react-router-dom";
import { LockKeyhole, Mail } from "lucide-react";
import { useAuth } from "../contexts/AuthContext";
import { apiErrorMessage } from "../lib/api";
import { Button, Card, TextInput } from "../components/ui";

export function LoginPage() {
  const { login, user } = useAuth();
  const [email, setEmail] = useState("admin@flowstock.local");
  const [password, setPassword] = useState("Admin@123456");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  if (user) {
    return <Navigate to={user.role === "SUPER_ADMIN" ? "/admin/dashboard" : "/app/dashboard"} replace />;
  }

  async function submit(event: FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError("");
    try {
      await login(email, password);
    } catch (err) {
      setError(apiErrorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  return (
    <main className="flex min-h-screen items-center justify-center bg-[linear-gradient(135deg,#102033_0%,#1557a6_55%,#168060_100%)] p-4">
      <Card className="w-full max-w-md">
        <div className="mb-8">
          <div className="text-2xl font-bold text-ink">FlowStock</div>
          <p className="mt-2 text-sm text-slate-500">Acesse seu painel operacional ou a area Super Admin.</p>
        </div>
        <form onSubmit={submit} className="space-y-4">
          <label className="block">
            <span className="mb-1 flex items-center gap-2 text-sm font-semibold"><Mail size={15} /> Email</span>
            <TextInput type="email" value={email} onChange={(event) => setEmail(event.target.value)} required />
          </label>
          <label className="block">
            <span className="mb-1 flex items-center gap-2 text-sm font-semibold"><LockKeyhole size={15} /> Senha</span>
            <TextInput type="password" value={password} onChange={(event) => setPassword(event.target.value)} required />
          </label>
          {error && <div className="rounded-md bg-red-50 px-3 py-2 text-sm font-medium text-red-700">{error}</div>}
          <Button className="w-full" disabled={busy}>{busy ? "Entrando..." : "Entrar"}</Button>
        </form>
      </Card>
    </main>
  );
}
