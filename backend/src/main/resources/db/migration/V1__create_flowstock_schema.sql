create table plans (
    id bigserial primary key,
    name varchar(80) not null unique,
    price numeric(12,2) not null,
    max_users integer not null,
    max_products integer not null,
    has_reports boolean not null,
    has_ai boolean not null,
    has_whatsapp boolean not null,
    has_backup boolean not null,
    active boolean not null default true
);

create table companies (
    id bigserial primary key,
    name varchar(140) not null,
    document varchar(32),
    email varchar(160) not null,
    phone varchar(32),
    status varchar(24) not null,
    plan_id bigint not null references plans(id),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    trial_ends_at date,
    subscription_ends_at date
);

create table users (
    id bigserial primary key,
    name varchar(140) not null,
    email varchar(160) not null unique,
    password_hash varchar(100) not null,
    role varchar(24) not null,
    company_id bigint references companies(id),
    active boolean not null default true,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    last_login_at timestamp with time zone
);

create table subscriptions (
    id bigserial primary key,
    company_id bigint not null references companies(id),
    plan_id bigint not null references plans(id),
    status varchar(24) not null,
    started_at date not null,
    ends_at date,
    trial boolean not null,
    payment_status varchar(24) not null
);

create table products (
    id bigserial primary key,
    company_id bigint not null references companies(id),
    name varchar(140) not null,
    description varchar(600),
    sku varchar(80),
    category varchar(80),
    unit varchar(20) not null,
    cost_price numeric(12,2) not null,
    sale_price numeric(12,2) not null,
    min_stock numeric(12,3) not null,
    current_stock numeric(12,3) not null,
    active boolean not null default true,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create table customers (
    id bigserial primary key,
    company_id bigint not null references companies(id),
    name varchar(140) not null,
    phone varchar(32),
    email varchar(160),
    document varchar(32),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create table stock_movements (
    id bigserial primary key,
    company_id bigint not null references companies(id),
    product_id bigint not null references products(id),
    type varchar(24) not null,
    quantity numeric(12,3) not null,
    reason varchar(300),
    created_by bigint references users(id),
    created_at timestamp with time zone not null
);

create table production_batches (
    id bigserial primary key,
    company_id bigint not null references companies(id),
    product_id bigint not null references products(id),
    quantity_produced numeric(12,3) not null,
    status varchar(24) not null,
    production_date date not null,
    notes varchar(600),
    created_by bigint references users(id),
    created_at timestamp with time zone not null
);

create table sales (
    id bigserial primary key,
    company_id bigint not null references companies(id),
    customer_id bigint references customers(id),
    total_amount numeric(12,2) not null,
    payment_method varchar(40) not null,
    status varchar(24) not null,
    created_by bigint references users(id),
    created_at timestamp with time zone not null
);

create table sale_items (
    id bigserial primary key,
    sale_id bigint not null references sales(id) on delete cascade,
    product_id bigint not null references products(id),
    quantity numeric(12,3) not null,
    unit_price numeric(12,2) not null,
    total_price numeric(12,2) not null
);

create table audit_logs (
    id bigserial primary key,
    company_id bigint references companies(id),
    actor_user_id bigint references users(id),
    action varchar(120) not null,
    entity_name varchar(80) not null,
    entity_id varchar(80),
    old_value text,
    new_value text,
    ip_address varchar(60),
    created_at timestamp with time zone not null
);

create table system_health_logs (
    id bigserial primary key,
    status varchar(24) not null,
    component varchar(80) not null,
    message varchar(600) not null,
    created_at timestamp with time zone not null
);

create table support_sessions (
    id bigserial primary key,
    super_admin_user_id bigint not null references users(id),
    company_id bigint not null references companies(id),
    reason varchar(600) not null,
    started_at timestamp with time zone not null,
    ended_at timestamp with time zone
);

create index idx_companies_status on companies(status);
create index idx_users_company on users(company_id);
create index idx_products_company on products(company_id);
create index idx_customers_company on customers(company_id);
create index idx_stock_movements_company on stock_movements(company_id);
create index idx_production_batches_company on production_batches(company_id);
create index idx_sales_company on sales(company_id);
create index idx_audit_logs_company on audit_logs(company_id);

insert into plans (name, price, max_users, max_products, has_reports, has_ai, has_whatsapp, has_backup, active)
values
    ('Starter', 49.90, 3, 300, false, false, false, false, true),
    ('Pro', 99.90, 10, 1500, true, false, false, false, true),
    ('Premium', 199.90, 50, 10000, true, true, true, true, true);

insert into users (name, email, password_hash, role, company_id, active, created_at, updated_at)
values (
    'Arthur Admin',
    'admin@flowstock.local',
    '$2a$10$dXJ3SW6G7P50lGmMkkIGbu6Q3jqqi3q.y1v5mLwM4vOL2aDeJ9WvO',
    'SUPER_ADMIN',
    null,
    true,
    now(),
    now()
);

insert into system_health_logs (status, component, message, created_at)
values ('ONLINE', 'API', 'Schema inicial criado com sucesso.', now());
