export type UserRole = "SUPER_ADMIN" | "COMPANY_ADMIN" | "EMPLOYEE";
export type CompanyStatus = "TRIAL" | "ACTIVE" | "PAST_DUE" | "SUSPENDED" | "CANCELED";

export interface UserMe {
  id: number;
  name: string;
  email: string;
  role: UserRole;
  companyId: number | null;
  companyName: string | null;
  companyStatus: CompanyStatus | null;
}

export interface Plan {
  id: number;
  name: string;
  price: number;
  maxUsers: number;
  maxProducts: number;
  hasReports: boolean;
  hasAi: boolean;
  hasWhatsapp: boolean;
  hasBackup: boolean;
  active: boolean;
}

export interface Company {
  id: number;
  name: string;
  document?: string;
  email: string;
  phone?: string;
  status: CompanyStatus;
  trialEndsAt?: string;
  subscriptionEndsAt?: string;
  plan: Plan;
  createdAt: string;
  updatedAt: string;
  usersCount?: number;
  lastAccessAt?: string;
}

export interface Product {
  id: number;
  name: string;
  description?: string;
  sku?: string;
  category?: string;
  unit: string;
  costPrice: number;
  salePrice: number;
  minStock: number;
  currentStock: number;
  active: boolean;
}

export interface User {
  id: number;
  name: string;
  email: string;
  role: UserRole;
  companyId: number | null;
  active: boolean;
  createdAt: string;
  lastLoginAt?: string;
}

export interface Customer {
  id: number;
  name: string;
  phone?: string;
  email?: string;
  document?: string;
  active: boolean;
}

export interface AuditLog {
  id: number;
  companyId: number | null;
  actor: string;
  action: string;
  entityName: string;
  entityId?: string;
  createdAt: string;
}

export interface SuperAdminDashboard {
  activeCompanies: number;
  trialCompanies: number;
  blockedCompanies: number;
  expectedMonthlyRevenue: number;
  overduePayments: number;
  recentErrors: number;
  apiStatus: string;
}

export interface CompanyDashboard {
  todaySalesCount: number;
  todayRevenue: number;
  productsCount: number;
  lowStockProductsCount: number;
  recentProductionsCount: number;
  recentStockMovements: StockMovement[];
}

export interface StockMovement {
  id: number;
  productId: number;
  productName: string;
  type: "IN" | "OUT" | "ADJUSTMENT" | "PRODUCTION" | "SALE";
  quantity: number;
  reason?: string;
  createdBy: string;
  createdAt: string;
}

export interface ProductionBatch {
  id: number;
  productId: number;
  productName: string;
  quantityProduced: number;
  status: "PLANNED" | "IN_PROGRESS" | "FINISHED" | "CANCELED";
  productionDate: string;
  notes?: string;
  createdBy: string;
  createdAt: string;
}

export interface Sale {
  id: number;
  customerId?: number;
  customerName?: string;
  totalAmount: number;
  paymentMethod: string;
  status: "OPEN" | "PAID" | "CANCELED";
  createdBy: string;
  createdAt: string;
  items: Array<{
    productId: number;
    productName: string;
    quantity: number;
    unitPrice: number;
    totalPrice: number;
  }>;
}

export interface ReportSummary {
  dashboard: CompanyDashboard;
  lowStockProducts: Product[];
  topProducts: Array<{ productName: string; quantity: number }>;
  recentProductions: ProductionBatch[];
  recentStockMovements: StockMovement[];
}
