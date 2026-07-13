import { FormEvent, useEffect, useState } from "react";
import { api, apiErrorMessage } from "../lib/api";
import type { AuditLog, Company, CompanyStatus, Plan, SuperAdminDashboard, User, UserRole } from "../types/api";
import { Button, Card, Select, StatCard, StatusPill, TextInput } from "../components/ui";

const money = new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" });

export function AdminDashboardPage() {
  const [data, setData] = useState<SuperAdminDashboard | null>(null);
  useEffect(() => { api.get("/admin/dashboard").then((r) => setData(r.data)); }, []);
  return (
    <Page title="Dashboard Super Admin">
      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        <StatCard label="Empresas ativas" value={data?.activeCompanies ?? "-"} tone="green" />
        <StatCard label="Empresas em teste" value={data?.trialCompanies ?? "-"} />
        <StatCard label="Empresas bloqueadas" value={data?.blockedCompanies ?? "-"} tone="red" />
        <StatCard label="Receita mensal prevista" value={data ? money.format(data.expectedMonthlyRevenue) : "-"} />
        <StatCard label="Pagamentos vencidos" value={data?.overduePayments ?? "-"} tone="red" />
        <StatCard label="Erros recentes" value={data?.recentErrors ?? "-"} tone="gray" />
        <StatCard label="Status da API" value={data?.apiStatus ?? "-"} tone="green" />
      </div>
    </Page>
  );
}

export function AdminCompaniesPage() {
  const [companies, setCompanies] = useState<Company[]>([]);
  const [plans, setPlans] = useState<Plan[]>([]);
  const [users, setUsers] = useState<User[]>([]);
  const [selectedCompany, setSelectedCompany] = useState<Company | null>(null);
  const [error, setError] = useState("");
  const [userError, setUserError] = useState("");
  const [form, setForm] = useState({
    name: "",
    email: "",
    phone: "",
    document: "",
    planId: "",
    status: "TRIAL" as CompanyStatus,
    adminName: "",
    adminEmail: "",
    adminPassword: "",
  });
  const [userForm, setUserForm] = useState({
    name: "",
    email: "",
    password: "",
    role: "EMPLOYEE" as Exclude<UserRole, "SUPER_ADMIN">,
  });

  const load = () => {
    api.get<Company[]>("/admin/companies").then((r) => setCompanies(r.data));
    api.get<Plan[]>("/admin/plans").then((r) => setPlans(r.data));
  };

  useEffect(load, []);

  async function create(event: FormEvent) {
    event.preventDefault();
    setError("");
    try {
      await api.post("/admin/companies", {
        ...form,
        planId: Number(form.planId),
      });
      setForm({
        name: "",
        email: "",
        phone: "",
        document: "",
        planId: "",
        status: "TRIAL",
        adminName: "",
        adminEmail: "",
        adminPassword: "",
      });
      load();
    } catch (err) {
      setError(apiErrorMessage(err));
    }
  }

  async function status(id: number, next: CompanyStatus) {
    if (next === "SUSPENDED" && !window.confirm("Bloquear esta empresa impede o acesso dos usuarios dela. Deseja continuar?")) {
      return;
    }
    await api.patch(`/admin/companies/${id}/status`, null, { params: { status: next } });
    load();
  }

  async function openUsers(company: Company) {
    setSelectedCompany(company);
    setUserError("");
    const response = await api.get<User[]>(`/admin/companies/${company.id}/users`);
    setUsers(response.data);
  }

  async function createUser(event: FormEvent) {
    event.preventDefault();
    if (!selectedCompany) return;
    setUserError("");
    try {
      await api.post(`/admin/companies/${selectedCompany.id}/users`, userForm);
      setUserForm({ name: "", email: "", password: "", role: "EMPLOYEE" });
      await openUsers(selectedCompany);
    } catch (err) {
      setUserError(apiErrorMessage(err));
    }
  }

  async function setUserActive(user: User, active: boolean) {
    if (!selectedCompany) return;
    await api.patch(`/admin/companies/${selectedCompany.id}/users/${user.id}/active`, null, { params: { active } });
    await openUsers(selectedCompany);
  }

  return (
    <Page title="Empresas">
      <Card>
        <form onSubmit={create} className="space-y-4">
          <div className="grid gap-3 lg:grid-cols-5">
            <TextInput placeholder="Nome da empresa" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} required />
            <TextInput placeholder="Email da empresa" type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} required />
            <TextInput placeholder="Telefone da empresa" value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} />
            <Select value={form.planId} onChange={(e) => setForm({ ...form, planId: e.target.value })} required>
              <option value="">Plano</option>
              {plans.map((plan) => <option key={plan.id} value={plan.id}>{plan.name}</option>)}
            </Select>
            <Select value={form.status} onChange={(e) => setForm({ ...form, status: e.target.value as CompanyStatus })}>
              <option value="TRIAL">TRIAL</option>
              <option value="ACTIVE">ACTIVE</option>
            </Select>
          </div>
          <div className="grid gap-3 lg:grid-cols-4">
            <TextInput placeholder="Nome do administrador" value={form.adminName} onChange={(e) => setForm({ ...form, adminName: e.target.value })} required />
            <TextInput placeholder="Email do administrador" type="email" value={form.adminEmail} onChange={(e) => setForm({ ...form, adminEmail: e.target.value })} required />
            <TextInput placeholder="Senha do administrador" type="password" value={form.adminPassword} onChange={(e) => setForm({ ...form, adminPassword: e.target.value })} required />
            <Button>Criar empresa</Button>
          </div>
        </form>
        {error && <div className="mt-3 text-sm font-medium text-danger">{error}</div>}
      </Card>

      <Table headers={["Nome", "Plano", "Status", "Usuários", "Criada em", "Último acesso", "Acoes"]}>
        {companies.map((company) => (
          <tr key={company.id}>
            <td className="font-semibold">{company.name}<div className="text-xs font-normal text-slate-500">{company.email}</div></td>
            <td>{company.plan.name}</td>
            <td><StatusPill value={company.status} /></td>
            <td>{company.usersCount ?? "-"}</td>
            <td>{new Date(company.createdAt).toLocaleDateString("pt-BR")}</td>
            <td>{company.lastAccessAt ? new Date(company.lastAccessAt).toLocaleString("pt-BR") : "-"}</td>
            <td className="flex flex-wrap gap-2">
              <Button variant="ghost" onClick={() => openUsers(company)}>Usuários</Button>
              <Button variant="ghost" title="Estrutura de suporte preparada para a próxima etapa" onClick={() => alert("Modo suporte preparado para a próxima etapa.")}>Suporte</Button>
              <Button variant="ghost" onClick={() => status(company.id, "SUSPENDED")}>Bloquear</Button>
              <Button variant="ghost" onClick={() => status(company.id, "ACTIVE")}>Liberar</Button>
              <Button variant="ghost" onClick={() => api.patch(`/admin/companies/${company.id}/extend-trial`, null, { params: { days: 7 } }).then(load)}>+7 dias</Button>
            </td>
          </tr>
        ))}
      </Table>

      {selectedCompany && (
        <Card>
          <div className="mb-4 flex flex-col justify-between gap-3 md:flex-row md:items-center">
            <div>
              <h2 className="text-lg font-bold">Usuários de {selectedCompany.name}</h2>
              <p className="text-sm text-slate-500">Crie administradores e funcionários vinculados somente a esta empresa.</p>
            </div>
            <Button variant="ghost" onClick={() => setSelectedCompany(null)}>Fechar</Button>
          </div>
          <form onSubmit={createUser} className="mb-4 grid gap-3 lg:grid-cols-5">
            <TextInput placeholder="Nome" value={userForm.name} onChange={(e) => setUserForm({ ...userForm, name: e.target.value })} required />
            <TextInput placeholder="Email" type="email" value={userForm.email} onChange={(e) => setUserForm({ ...userForm, email: e.target.value })} required />
            <TextInput placeholder="Senha" type="password" value={userForm.password} onChange={(e) => setUserForm({ ...userForm, password: e.target.value })} required />
            <Select value={userForm.role} onChange={(e) => setUserForm({ ...userForm, role: e.target.value as Exclude<UserRole, "SUPER_ADMIN"> })}>
              <option value="COMPANY_ADMIN">COMPANY_ADMIN</option>
              <option value="EMPLOYEE">EMPLOYEE</option>
            </Select>
            <Button>Criar usuário</Button>
          </form>
          {userError && <div className="mb-3 text-sm font-medium text-danger">{userError}</div>}
          <div className="overflow-x-auto">
            <table className="w-full min-w-[680px] text-left text-sm">
              <thead className="bg-slate-50 text-xs uppercase text-slate-500">
                <tr><th className="px-4 py-3">Nome</th><th className="px-4 py-3">Email</th><th className="px-4 py-3">Role</th><th className="px-4 py-3">Status</th><th className="px-4 py-3">Acoes</th></tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {users.map((user) => (
                  <tr key={user.id}>
                    <td className="font-semibold">{user.name}</td>
                    <td>{user.email}</td>
                    <td>{user.role}</td>
                    <td><StatusPill value={user.active ? "ACTIVE" : "INACTIVE"} /></td>
                    <td>
                      <Button variant="ghost" onClick={() => setUserActive(user, !user.active)}>
                        {user.active ? "Desativar" : "Ativar"}
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Card>
      )}
    </Page>
  );
}

export function AdminPlansPage() {
  const [plans, setPlans] = useState<Plan[]>([]);
  useEffect(() => { api.get("/admin/plans").then((r) => setPlans(r.data)); }, []);
  return (
    <Page title="Planos">
      <Table headers={["Nome", "Preco", "Usuarios", "Produtos", "Recursos", "Status"]}>
        {plans.map((plan) => (
          <tr key={plan.id}>
            <td className="font-semibold">{plan.name}</td>
            <td>{money.format(plan.price)}</td>
            <td>{plan.maxUsers}</td>
            <td>{plan.maxProducts}</td>
            <td>{[plan.hasReports && "Relatorios", plan.hasAi && "IA", plan.hasWhatsapp && "WhatsApp", plan.hasBackup && "Backup"].filter(Boolean).join(", ") || "Essenciais"}</td>
            <td><StatusPill value={plan.active ? "ACTIVE" : "INACTIVE"} /></td>
          </tr>
        ))}
      </Table>
    </Page>
  );
}

export function AdminLogsPage() {
  const [logs, setLogs] = useState<AuditLog[]>([]);
  useEffect(() => { api.get("/admin/logs").then((r) => setLogs(r.data)); }, []);
  return (
    <Page title="Logs de auditoria">
      <Table headers={["Data", "Ator", "Acao", "Entidade", "Empresa"]}>
        {logs.map((log) => (
          <tr key={log.id}>
            <td>{new Date(log.createdAt).toLocaleString("pt-BR")}</td>
            <td>{log.actor}</td>
            <td className="font-semibold">{log.action}</td>
            <td>{log.entityName} #{log.entityId}</td>
            <td>{log.companyId ?? "Global"}</td>
          </tr>
        ))}
      </Table>
    </Page>
  );
}

export function AdminHealthPage() {
  const [rows, setRows] = useState<Array<{ status: string; component: string; message: string; checkedAt: string }>>([]);
  useEffect(() => { api.get("/admin/health").then((r) => setRows(r.data)); }, []);
  return (
    <Page title="Status do sistema">
      <Table headers={["Componente", "Status", "Mensagem", "Verificado em"]}>
        {rows.map((row, index) => (
          <tr key={index}>
            <td className="font-semibold">{row.component}</td>
            <td><StatusPill value={row.status} /></td>
            <td>{row.message}</td>
            <td>{new Date(row.checkedAt).toLocaleString("pt-BR")}</td>
          </tr>
        ))}
      </Table>
    </Page>
  );
}

function Page({ title, children }: { title: string; children: React.ReactNode }) {
  return <div className="space-y-5"><h1 className="text-2xl font-bold text-ink">{title}</h1>{children}</div>;
}

function Table({ headers, children }: { headers: string[]; children: React.ReactNode }) {
  return (
    <Card className="overflow-x-auto p-0">
      <table className="w-full min-w-[760px] text-left text-sm">
        <thead className="bg-slate-50 text-xs uppercase text-slate-500">
          <tr>{headers.map((header) => <th key={header} className="px-4 py-3 font-bold">{header}</th>)}</tr>
        </thead>
        <tbody className="divide-y divide-slate-100">{children}</tbody>
      </table>
    </Card>
  );
}
