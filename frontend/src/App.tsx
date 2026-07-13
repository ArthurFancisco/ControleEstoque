import { Navigate, Route, Routes } from "react-router-dom";
import { AppLayout } from "./components/AppLayout";
import { useAuth } from "./contexts/AuthContext";
import { LoginPage } from "./pages/LoginPage";
import { AdminCompaniesPage, AdminDashboardPage, AdminHealthPage, AdminLogsPage, AdminPlansPage } from "./pages/admin";
import { CompanyCustomersPage, CompanyDashboardPage, CompanyProductionPage, CompanyProductsPage, CompanyReportsPage, CompanySalesPage, CompanyStockPage } from "./pages/company";

function Protected({ area }: { area: "admin" | "company" }) {
  const { user, loading } = useAuth();
  if (loading) return <div className="p-8 text-sm text-slate-500">Carregando...</div>;
  if (!user) return <Navigate to="/login" replace />;
  if (area === "admin" && user.role !== "SUPER_ADMIN") return <Navigate to="/app/dashboard" replace />;
  if (area === "company" && user.role === "SUPER_ADMIN") return <Navigate to="/admin/dashboard" replace />;
  return <AppLayout />;
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route element={<Protected area="admin" />}>
        <Route path="/admin" element={<Navigate to="/admin/dashboard" replace />} />
        <Route path="/admin/dashboard" element={<AdminDashboardPage />} />
        <Route path="/admin/companies" element={<AdminCompaniesPage />} />
        <Route path="/admin/plans" element={<AdminPlansPage />} />
        <Route path="/admin/logs" element={<AdminLogsPage />} />
        <Route path="/admin/health" element={<AdminHealthPage />} />
      </Route>
      <Route element={<Protected area="company" />}>
        <Route path="/app" element={<Navigate to="/app/dashboard" replace />} />
        <Route path="/app/dashboard" element={<CompanyDashboardPage />} />
        <Route path="/app/products" element={<CompanyProductsPage />} />
        <Route path="/app/stock" element={<CompanyStockPage />} />
        <Route path="/app/production" element={<CompanyProductionPage />} />
        <Route path="/app/sales" element={<CompanySalesPage />} />
        <Route path="/app/customers" element={<CompanyCustomersPage />} />
        <Route path="/app/reports" element={<CompanyReportsPage />} />
      </Route>
      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  );
}
