import { BarChart3, Boxes, Building2, ClipboardList, Factory, FileText, HeartPulse, LogOut, Package, ShoppingCart, Users } from "lucide-react";
import { NavLink, Outlet } from "react-router-dom";
import { useAuth } from "../contexts/AuthContext";
import { Button } from "./ui";
import { clsx } from "clsx";

const adminItems = [
  { to: "/admin/dashboard", label: "Dashboard", icon: BarChart3 },
  { to: "/admin/companies", label: "Empresas", icon: Building2 },
  { to: "/admin/plans", label: "Planos", icon: ClipboardList },
  { to: "/admin/logs", label: "Logs", icon: FileText },
  { to: "/admin/health", label: "Sistema", icon: HeartPulse },
];

const companyItems = [
  { to: "/app/dashboard", label: "Dashboard", icon: BarChart3 },
  { to: "/app/products", label: "Produtos", icon: Package },
  { to: "/app/stock", label: "Estoque", icon: Boxes },
  { to: "/app/production", label: "Producao", icon: Factory },
  { to: "/app/sales", label: "Vendas", icon: ShoppingCart },
  { to: "/app/customers", label: "Clientes", icon: Users },
  { to: "/app/reports", label: "Relatorios", icon: FileText },
];

export function AppLayout() {
  const { user, logout } = useAuth();
  const items = user?.role === "SUPER_ADMIN" ? adminItems : companyItems;
  const sidebarSubtitle = user?.role === "SUPER_ADMIN" ? "Painel da plataforma" : "Painel da empresa";
  const isAdmin = user?.role === "SUPER_ADMIN";

  return (
    <div className="min-h-screen bg-surface">
      <aside className={clsx("fixed inset-y-0 left-0 z-20 hidden w-64 border-r border-slate-200 text-white lg:block", isAdmin ? "bg-ink" : "bg-brand")}>
        <div className="flex h-16 items-center px-6">
          <div>
            <div className="text-xl font-bold">FlowStock</div>
            <div className="text-xs text-slate-300">{sidebarSubtitle}</div>
          </div>
        </div>
        <nav className="mt-4 space-y-1 px-3">
          {items.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.to === "/admin/dashboard" || item.to === "/app/dashboard"}
              className={({ isActive }) =>
                `flex items-center gap-3 rounded-md px-3 py-2.5 text-sm font-medium transition ${isActive ? "bg-white text-ink" : "text-slate-200 hover:bg-white/10"}`
              }
            >
              <item.icon size={18} />
              {item.label}
            </NavLink>
          ))}
        </nav>
        <div className="absolute bottom-4 left-3 right-3">
          <button
            className="flex w-full items-center gap-3 rounded-md px-3 py-2.5 text-sm font-medium text-slate-200 transition hover:bg-white/10"
            onClick={logout}
            type="button"
          >
            <LogOut size={18} />
            Sair
          </button>
        </div>
      </aside>

      <div className="lg:pl-64">
        <header className="sticky top-0 z-10 flex min-h-16 items-center justify-between border-b border-slate-200 bg-white px-4 lg:px-8">
          <div>
            <div className="text-sm font-semibold text-ink">{user?.companyName ?? "Painel da Plataforma"}</div>
            <div className="text-xs text-slate-500">{user?.name} - {user?.role}</div>
          </div>
          <Button variant="ghost" onClick={logout}>
            <LogOut size={16} /> Sair
          </Button>
        </header>
        <main className="mx-auto max-w-7xl px-4 py-6 lg:px-8">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
