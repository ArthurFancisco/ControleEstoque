import { FormEvent, ReactNode, useEffect, useMemo, useState } from "react";
import { api, apiErrorMessage } from "../lib/api";
import type { CompanyDashboard, Customer, Product, ProductionBatch, ReportSummary, Sale, StockMovement } from "../types/api";
import { Button, Card, EmptyState, Select, StatCard, StatusPill, TextInput } from "../components/ui";

const money = new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" });
const unitOptions = ["UNIDADE", "GARRAFA", "COPO", "LITRO", "KG", "CAIXA", "PACOTE"];

export function CompanyDashboardPage() {
  const [data, setData] = useState<CompanyDashboard | null>(null);
  const [error, setError] = useState("");

  useEffect(() => {
    api.get<CompanyDashboard>("/api/app/dashboard")
      .then((r) => setData(r.data))
      .catch((err) => setError(apiErrorMessage(err)));
  }, []);

  return (
    <Page title="Dashboard da empresa" error={error}>
      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-5">
        <StatCard label="Vendas hoje" value={data?.todaySalesCount ?? "-"} />
        <StatCard label="Faturamento hoje" value={data ? money.format(data.todayRevenue) : "-"} tone="green" />
        <StatCard label="Produtos cadastrados" value={data?.productsCount ?? "-"} />
        <StatCard label="Produtos com estoque baixo" value={data?.lowStockProductsCount ?? "-"} tone="red" />
        <StatCard label="Producoes recentes" value={data?.recentProductionsCount ?? "-"} />
      </div>
      <Card>
        <SectionTitle title="Ultimas movimentacoes de estoque" />
        {!data?.recentStockMovements?.length ? (
          <EmptyState title="Sem movimentacoes ainda" text="Entradas, saidas, producao e vendas aparecerao aqui." />
        ) : (
          <SimpleMovementsTable rows={data.recentStockMovements} />
        )}
      </Card>
    </Page>
  );
}

export function CompanyProductsPage() {
  const [products, setProducts] = useState<Product[]>([]);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [search, setSearch] = useState("");
  const [status, setStatus] = useState("ALL");
  const [showForm, setShowForm] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [busy, setBusy] = useState(false);
  const [form, setForm] = useState(emptyProductForm());

  const load = () => api.get<Product[]>("/api/app/products").then((r) => setProducts(r.data)).catch((err) => setError(apiErrorMessage(err)));
  useEffect(() => { load(); }, []);

  const filtered = useMemo(() => products.filter((product) => {
    const term = search.toLowerCase();
    const matchesSearch = [product.name, product.sku, product.category].some((value) => (value ?? "").toLowerCase().includes(term));
    const matchesStatus = status === "ALL" || (status === "ACTIVE" ? product.active : !product.active);
    return matchesSearch && matchesStatus;
  }), [products, search, status]);

  const productWarnings = getProductWarnings(form);

  async function submit(event: FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError("");
    setSuccess("");

    const validationError = validateProductForm(form);
    if (validationError) {
      setError(validationError);
      setBusy(false);
      return;
    }

    if (productWarnings.priceBelowCost && !window.confirm("Atencao: o preco de venda esta menor que o preco de custo. Deseja salvar mesmo assim?")) {
      setBusy(false);
      return;
    }

    try {
      if (editingId) {
        await api.put(`/api/app/products/${editingId}`, numericProduct(form));
        setSuccess("Produto atualizado.");
      } else {
        await api.post("/api/app/products", numericProduct(form));
        setSuccess("Produto criado.");
      }
      reset();
      load();
    } catch (err) {
      setError(apiErrorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  function edit(product: Product) {
    setShowForm(true);
    setEditingId(product.id);
    setForm({
      name: product.name,
      description: product.description ?? "",
      sku: product.sku ?? "",
      category: product.category ?? "",
      unit: product.unit,
      costPrice: String(product.costPrice),
      salePrice: String(product.salePrice),
      minStock: String(product.minStock),
      currentStock: String(product.currentStock),
      active: String(product.active),
    });
  }

  function reset() {
    setEditingId(null);
    setShowForm(false);
    setForm(emptyProductForm());
  }

  async function toggle(product: Product) {
    const action = product.active ? "desativar" : "ativar";
    if (!window.confirm(`Deseja ${action} o produto ${product.name}?`)) return;
    try {
      await api.patch(`/api/app/products/${product.id}/toggle-active`);
      setSuccess(product.active ? "Produto desativado." : "Produto ativado.");
      load();
    } catch (err) {
      setError(apiErrorMessage(err));
    }
  }

  return (
    <Page title="Produtos" error={error} success={success}>
      <InfoBox>
        Cadastre os produtos uma vez. Depois, use Estoque para entrada ou ajuste de mercadoria e Producao para itens fabricados.
      </InfoBox>

      <Card>
        <div className="flex flex-col justify-between gap-3 lg:flex-row lg:items-center">
          <div className="grid flex-1 gap-3 md:grid-cols-[1fr_180px]">
            <TextInput placeholder="Buscar por nome, SKU ou categoria" value={search} onChange={(e) => setSearch(e.target.value)} />
            <Select value={status} onChange={(e) => setStatus(e.target.value)}>
              <option value="ALL">Todos</option>
              <option value="ACTIVE">Ativos</option>
              <option value="INACTIVE">Inativos</option>
            </Select>
          </div>
          <Button onClick={() => { setShowForm(true); setEditingId(null); setForm(emptyProductForm()); }}>Novo produto</Button>
        </div>
      </Card>

      {showForm && (
        <Card>
          <SectionTitle title={editingId ? "Editar produto" : "Novo produto"} />
          <form onSubmit={submit} className="space-y-5">
            <FormSection title="1. Identificacao">
              <Field label="Nome do produto" help="Exemplo: Drink Maracuja 500ml">
                <TextInput value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} required />
              </Field>
              <Field label="Descricao" help="Opcional. Use para detalhes como sabor, tamanho ou embalagem.">
                <TextInput value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} />
              </Field>
              <Field label="SKU" help="Codigo interno opcional. Exemplo: DRINK-MARACUJA-500">
                <TextInput value={form.sku} onChange={(e) => setForm({ ...form, sku: e.target.value })} />
              </Field>
              <Field label="Categoria" help="Exemplo: Drinks, Batidas, Acai, Sobremesas, Marmitas">
                <TextInput value={form.category} onChange={(e) => setForm({ ...form, category: e.target.value })} />
              </Field>
              <Field label="Unidade" help="Como este produto e contado no estoque.">
                <Select value={form.unit} onChange={(e) => setForm({ ...form, unit: e.target.value })}>
                  {unitOptions.map((unit) => <option key={unit} value={unit}>{unit}</option>)}
                </Select>
              </Field>
            </FormSection>

            <FormSection title="2. Precos">
              <Field label="Preco de custo" help="Quanto a empresa gasta para produzir ou comprar 1 unidade.">
                <TextInput type="number" min="0" step="0.01" value={form.costPrice} onChange={(e) => setForm({ ...form, costPrice: e.target.value })} />
              </Field>
              <Field label="Preco de venda" help="Por quanto a empresa vende 1 unidade.">
                <TextInput type="number" min="0" step="0.01" value={form.salePrice} onChange={(e) => setForm({ ...form, salePrice: e.target.value })} />
              </Field>
            </FormSection>

            <FormSection title="3. Estoque">
              <Field label="Estoque minimo" help="Quando o estoque chegar nesse numero, o sistema mostrara alerta.">
                <TextInput type="number" min="0" step="0.001" value={form.minStock} onChange={(e) => setForm({ ...form, minStock: e.target.value })} />
              </Field>
              <Field label="Estoque atual" help="Quantidade disponivel agora. Para novas entradas, prefira usar a tela Estoque.">
                <TextInput type="number" min="0" step="0.001" value={form.currentStock} onChange={(e) => setForm({ ...form, currentStock: e.target.value })} />
              </Field>
            </FormSection>

            <FormSection title="4. Status">
              <Field label="Situacao do produto" help="Produtos inativos nao devem ser usados em novas vendas.">
                <Select value={form.active} onChange={(e) => setForm({ ...form, active: e.target.value })}>
                  <option value="true">Ativo</option>
                  <option value="false">Inativo</option>
                </Select>
              </Field>
            </FormSection>

            <ProductSummary form={form} warnings={productWarnings} />

            <div className="flex flex-wrap gap-3">
              <Button disabled={busy}>{busy ? "Salvando..." : "Salvar produto"}</Button>
              <Button type="button" variant="ghost" onClick={reset}>Cancelar</Button>
            </div>
          </form>
        </Card>
      )}

      <Table
        headers={["Produto", "SKU", "Categoria", "Preco de venda", "Estoque atual", "Status", "Acoes"]}
        empty={!filtered.length}
        emptyTitle="Nenhum produto cadastrado ainda"
        emptyText="Cadastre seu primeiro produto para comecar a controlar o estoque."
      >
        {filtered.map((product) => {
          const lowStock = product.currentStock <= product.minStock;
          return (
            <tr key={product.id}>
              <td className="font-semibold">
                {product.name}
                <div className="text-xs font-normal text-slate-500">{product.description ?? ""}</div>
                {lowStock && <div className="mt-1 text-xs font-bold text-danger">Estoque baixo</div>}
              </td>
              <td>{product.sku || "-"}</td>
              <td>{product.category || "-"}</td>
              <td>{money.format(product.salePrice)}</td>
              <td className={lowStock ? "font-bold text-danger" : ""}>{product.currentStock} {product.unit}</td>
              <td><StatusPill value={product.active ? "ACTIVE" : "INACTIVE"} /></td>
              <td className="flex flex-wrap gap-2">
                <Button variant="ghost" onClick={() => edit(product)}>Editar</Button>
                <Button variant={product.active ? "danger" : "ghost"} onClick={() => toggle(product)}>{product.active ? "Desativar" : "Ativar"}</Button>
              </td>
            </tr>
          );
        })}
      </Table>
    </Page>
  );
}

export function CompanyStockPage() {
  const [products, setProducts] = useState<Product[]>([]);
  const [movements, setMovements] = useState<StockMovement[]>([]);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [busy, setBusy] = useState(false);
  const [form, setForm] = useState({ productId: "", type: "IN", quantity: "1", reason: "" });

  const selectedProduct = products.find((product) => product.id === Number(form.productId));
  const stockAfter = selectedProduct ? calculateStockAfter(selectedProduct.currentStock, form.type, Number(form.quantity)) : null;

  const load = () => {
    api.get<Product[]>("/api/app/stock").then((r) => setProducts(r.data)).catch((err) => setError(apiErrorMessage(err)));
    api.get<StockMovement[]>("/api/app/stock/movements").then((r) => setMovements(r.data)).catch((err) => setError(apiErrorMessage(err)));
  };
  useEffect(load, []);

  async function move(event: FormEvent) {
    event.preventDefault();
    if (!selectedProduct) {
      setError("Selecione um produto.");
      return;
    }
    if (stockAfter === null || stockAfter < 0) {
      setError("Esta movimentacao deixaria o estoque negativo.");
      return;
    }
    const confirmed = window.confirm(
      `Confirme esta movimentacao. Ela ira alterar o estoque deste produto.\n\nProduto: ${selectedProduct.name}\nEstoque atual: ${selectedProduct.currentStock}\nTipo: ${movementLabel(form.type)}\nQuantidade: ${form.quantity}\nEstoque depois: ${stockAfter}\nMotivo: ${form.reason || "-"}`
    );
    if (!confirmed) return;

    setBusy(true);
    setError("");
    setSuccess("");
    try {
      await api.post("/api/app/stock/movements", { ...form, productId: Number(form.productId), quantity: Number(form.quantity) });
      setForm({ productId: "", type: "IN", quantity: "1", reason: "" });
      setSuccess("Movimentacao registrada.");
      load();
    } catch (err) {
      setError(apiErrorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  return (
    <Page title="Estoque" error={error} success={success}>
      <InfoBox>
        Entrada soma no estoque. Saida e usada para perda, descarte ou retirada manual. Ajuste corrige o estoque quando a contagem fisica esta diferente.
        Producao aumenta estoque ao finalizar um lote. Venda baixa estoque automaticamente quando for paga.
      </InfoBox>

      <Card>
        {!products.length ? (
          <EmptyState title="Cadastre produtos antes de movimentar o estoque" text="Depois de criar produtos, voce podera registrar entradas, saidas e ajustes." />
        ) : (
          <form onSubmit={move} className="space-y-4">
            <div className="grid gap-3 lg:grid-cols-4">
              <Field label="Produto" help="Escolha o produto que tera o estoque alterado.">
                <Select value={form.productId} onChange={(e) => setForm({ ...form, productId: e.target.value })} required>
                  <option value="">Selecione</option>
                  {products.map((product) => <option key={product.id} value={product.id}>{product.name} ({product.currentStock} {product.unit})</option>)}
                </Select>
              </Field>
              <Field label="Tipo de movimentacao" help="Escolha entrada, saida ou ajuste.">
                <Select value={form.type} onChange={(e) => setForm({ ...form, type: e.target.value })}>
                  <option value="IN">Entrada</option>
                  <option value="OUT">Saida</option>
                  <option value="ADJUSTMENT">Ajuste</option>
                </Select>
              </Field>
              <Field label="Quantidade" help={form.type === "ADJUSTMENT" ? "No ajuste, informe qual deve ser o estoque final." : "Informe quantas unidades entram ou saem."}>
                <TextInput type="number" min="0.001" step="0.001" value={form.quantity} onChange={(e) => setForm({ ...form, quantity: e.target.value })} />
              </Field>
              <Field label="Motivo" help="Exemplo: compra de mercadoria, perda, contagem fisica.">
                <TextInput value={form.reason} onChange={(e) => setForm({ ...form, reason: e.target.value })} />
              </Field>
            </div>
            {selectedProduct && (
              <SummaryBox>
                <strong>Resumo:</strong> {selectedProduct.name} vai de {selectedProduct.currentStock} para {stockAfter ?? "-"} {selectedProduct.unit}.
              </SummaryBox>
            )}
            <Button disabled={busy}>{busy ? "Registrando..." : "Registrar movimentacao"}</Button>
          </form>
        )}
      </Card>

      <Table headers={["Produto", "Estoque atual", "Minimo", "Status"]} empty={!products.length} emptyTitle="Nenhum produto cadastrado" emptyText="Cadastre produtos antes de movimentar o estoque.">
        {products.map((product) => <tr key={product.id}><td className="font-semibold">{product.name}</td><td>{product.currentStock} {product.unit}</td><td>{product.minStock}</td><td>{product.currentStock <= product.minStock ? <StatusPill value="PAST_DUE" /> : <StatusPill value="ACTIVE" />}</td></tr>)}
      </Table>

      <Card>
        <SectionTitle title="Historico de movimentacoes" />
        {!movements.length ? <EmptyState title="Sem historico" text="As movimentacoes de estoque aparecerao aqui." /> : <SimpleMovementsTable rows={movements} />}
      </Card>
    </Page>
  );
}

export function CompanyProductionPage() {
  const [products, setProducts] = useState<Product[]>([]);
  const [rows, setRows] = useState<ProductionBatch[]>([]);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [busy, setBusy] = useState(false);
  const [form, setForm] = useState({ productId: "", quantityProduced: "1", productionDate: new Date().toISOString().slice(0, 10), notes: "" });

  const load = () => {
    api.get<Product[]>("/api/app/products").then((r) => setProducts(r.data)).catch((err) => setError(apiErrorMessage(err)));
    api.get<ProductionBatch[]>("/api/app/production").then((r) => setRows(r.data)).catch((err) => setError(apiErrorMessage(err)));
  };
  useEffect(load, []);

  async function create(event: FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError("");
    setSuccess("");
    try {
      await api.post("/api/app/production", { ...form, productId: Number(form.productId), quantityProduced: Number(form.quantityProduced) });
      setForm({ productId: "", quantityProduced: "1", productionDate: new Date().toISOString().slice(0, 10), notes: "" });
      setSuccess("Producao criada. O estoque so aumenta quando ela for finalizada.");
      load();
    } catch (err) {
      setError(apiErrorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  async function action(row: ProductionBatch, type: "finish" | "cancel") {
    if (type === "finish" && row.status === "CANCELED") {
      setError("Producao cancelada nao pode ser finalizada.");
      return;
    }
    if (type === "cancel" && row.status === "FINISHED") {
      setError("Producao finalizada nao pode ser cancelada.");
      return;
    }
    const message = type === "finish"
      ? `Finalizar esta producao adicionara ${row.quantityProduced} unidades ao estoque do produto ${row.productName}. Deseja confirmar?`
      : `Deseja cancelar a producao de ${row.productName}?`;
    if (!window.confirm(message)) return;
    try {
      await api.patch(`/api/app/production/${row.id}/${type}`);
      setSuccess(type === "finish" ? "Producao finalizada e estoque atualizado." : "Producao cancelada.");
      load();
    } catch (err) {
      setError(apiErrorMessage(err));
    }
  }

  return (
    <Page title="Producao" error={error} success={success}>
      <InfoBox>
        Criar uma producao nao altera o estoque automaticamente. O estoque so sera aumentado quando a producao for finalizada.
      </InfoBox>
      <Card>
        <form onSubmit={create} className="grid gap-3 lg:grid-cols-5">
          <Field label="Produto produzido" help="Produto que entrara no estoque ao finalizar.">
            <Select value={form.productId} onChange={(e) => setForm({ ...form, productId: e.target.value })} required>
              <option value="">Selecione</option>
              {products.map((product) => <option key={product.id} value={product.id}>{product.name}</option>)}
            </Select>
          </Field>
          <Field label="Quantidade produzida" help="Quantidade que sera adicionada ao estoque ao finalizar.">
            <TextInput type="number" min="0.001" step="0.001" value={form.quantityProduced} onChange={(e) => setForm({ ...form, quantityProduced: e.target.value })} />
          </Field>
          <Field label="Data de producao" help="Data planejada ou realizada.">
            <TextInput type="date" value={form.productionDate} onChange={(e) => setForm({ ...form, productionDate: e.target.value })} />
          </Field>
          <Field label="Observacoes" help="Opcional. Exemplo: lote da semana.">
            <TextInput value={form.notes} onChange={(e) => setForm({ ...form, notes: e.target.value })} />
          </Field>
          <div className="flex items-end"><Button disabled={busy}>{busy ? "Criando..." : "Criar producao"}</Button></div>
        </form>
      </Card>
      <Table headers={["Produto", "Qtd.", "Status", "Data", "Acoes"]} empty={!rows.length} emptyTitle="Nenhuma producao registrada ainda" emptyText="Crie uma producao para planejar ou registrar novos itens fabricados.">
        {rows.map((row) => (
          <tr key={row.id}>
            <td className="font-semibold">{row.productName}</td>
            <td>{row.quantityProduced}</td>
            <td><StatusPill value={row.status} /></td>
            <td>{row.productionDate}</td>
            <td className="flex gap-2">
              <Button variant="ghost" disabled={row.status === "FINISHED" || row.status === "CANCELED"} onClick={() => action(row, "finish")}>Finalizar</Button>
              <Button variant="danger" disabled={row.status === "FINISHED" || row.status === "CANCELED"} onClick={() => action(row, "cancel")}>Cancelar</Button>
            </td>
          </tr>
        ))}
      </Table>
    </Page>
  );
}

export function CompanySalesPage() {
  const [products, setProducts] = useState<Product[]>([]);
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [sales, setSales] = useState<Sale[]>([]);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [busy, setBusy] = useState(false);
  const [form, setForm] = useState({ customerId: "", productId: "", quantity: "1", paymentMethod: "PIX", status: "PAID" });
  const [saleItems, setSaleItems] = useState<SaleDraftItem[]>([]);

  const load = () => {
    api.get<Product[]>("/api/app/products").then((r) => setProducts(r.data)).catch((err) => setError(apiErrorMessage(err)));
    api.get<Customer[]>("/api/app/customers").then((r) => setCustomers(r.data.filter((customer) => customer.active))).catch((err) => setError(apiErrorMessage(err)));
    api.get<Sale[]>("/api/app/sales").then((r) => setSales(r.data)).catch((err) => setError(apiErrorMessage(err)));
  };
  useEffect(load, []);

  async function create(event: FormEvent) {
    event.preventDefault();
    setError("");
    setSuccess("");
    if (!saleItems.length) {
      setError("Adicione pelo menos um produto a venda.");
      return;
    }
    const summary = saleItems.map((item) => `${item.productName}: ${item.quantity} un. | estoque ${item.stockBefore} -> ${item.stockAfter}`).join("\n");
    const confirmText = form.status === "PAID"
      ? `Essa venda ira baixar o estoque automaticamente. Deseja confirmar?\n\n${summary}\n\nTotal: ${money.format(saleItems.reduce((sum, item) => sum + item.totalPrice, 0))}`
      : "Venda em aberto nao baixa estoque. Deseja criar esta venda?";
    if (!window.confirm(confirmText)) return;

    setBusy(true);
    try {
      await api.post("/api/app/sales", {
        customerId: form.customerId ? Number(form.customerId) : null,
        paymentMethod: form.paymentMethod,
        status: form.status,
        items: saleItems.map((item) => ({ productId: item.productId, quantity: item.quantity, unitPrice: item.unitPrice }))
      });
      setForm({ customerId: "", productId: "", quantity: "1", paymentMethod: "PIX", status: "PAID" });
      setSaleItems([]);
      setSuccess("Venda registrada.");
      load();
    } catch (err) {
      setError(apiErrorMessage(err));
    } finally {
      setBusy(false);
    }
  }

  function addSaleItem() {
    const product = products.find((item) => item.id === Number(form.productId));
    const quantity = Number(form.quantity);
    if (!product || !quantity || quantity <= 0) {
      setError("Selecione um produto e uma quantidade valida.");
      return;
    }
    if (quantity > product.currentStock) {
      setError(`Nao e possivel vender ${quantity}. Estoque disponivel: ${product.currentStock}.`);
      return;
    }
    setError("");
    setSaleItems((items) => [
      ...items,
      {
        productId: product.id,
        productName: product.name,
        quantity,
        unitPrice: product.salePrice,
        totalPrice: product.salePrice * quantity,
        stockBefore: product.currentStock,
        stockAfter: product.currentStock - quantity,
      },
    ]);
    setForm((current) => ({ ...current, productId: "", quantity: "1" }));
  }

  async function action(sale: Sale, actionName: "pay" | "cancel") {
    const message = actionName === "pay"
      ? "Venda paga baixa estoque automaticamente. Deseja confirmar?"
      : sale.status === "PAID"
        ? "Esta venda ja baixou estoque. Ao cancelar, os itens serao devolvidos ao estoque. Deseja confirmar?"
        : "Deseja cancelar esta venda?";
    if (!window.confirm(message)) return;
    try {
      await api.patch(`/api/app/sales/${sale.id}/${actionName}`);
      setSuccess(actionName === "pay" ? "Venda paga." : "Venda cancelada.");
      load();
    } catch (err) {
      setError(apiErrorMessage(err));
    }
  }

  return (
    <Page title="Vendas" error={error} success={success}>
      <InfoBox>
        Venda em aberto nao baixa estoque. Venda paga baixa estoque automaticamente.
      </InfoBox>
      <Card>
        <form onSubmit={create} className="space-y-3">
          <div className="grid gap-3 lg:grid-cols-5">
            <Field label="Cliente" help="Opcional. Voce tambem pode vender para cliente avulso.">
              <Select value={form.customerId} onChange={(e) => setForm({ ...form, customerId: e.target.value })}>
                <option value="">Cliente avulso</option>
                {customers.map((customer) => <option key={customer.id} value={customer.id}>{customer.name}</option>)}
              </Select>
            </Field>
            <Field label="Forma de pagamento" help="Como a venda foi ou sera paga.">
              <Select value={form.paymentMethod} onChange={(e) => setForm({ ...form, paymentMethod: e.target.value })}>
                <option value="CASH">Dinheiro</option><option value="PIX">PIX</option><option value="CREDIT_CARD">Credito</option><option value="DEBIT_CARD">Debito</option><option value="OTHER">Outro</option>
              </Select>
            </Field>
            <Field label="Status da venda" help="Paga baixa estoque. Aberta nao baixa estoque agora.">
              <Select value={form.status} onChange={(e) => setForm({ ...form, status: e.target.value })}>
                <option value="PAID">Venda paga</option><option value="OPEN">Venda em aberto</option>
              </Select>
            </Field>
            <SummaryBox className="lg:col-span-2">Total da venda: {money.format(saleItems.reduce((sum, item) => sum + item.totalPrice, 0))}</SummaryBox>
          </div>
          <div className="grid gap-3 lg:grid-cols-[1fr_160px_170px]">
            <Field label="Produto" help="Mostramos o estoque disponivel para evitar erro.">
              <Select value={form.productId} onChange={(e) => setForm({ ...form, productId: e.target.value })}>
                <option value="">Selecione</option>
                {products.filter((product) => product.active).map((product) => <option key={product.id} value={product.id}>{product.name} - estoque {product.currentStock}</option>)}
              </Select>
            </Field>
            <Field label="Quantidade" help="Nao pode passar do estoque disponivel.">
              <TextInput type="number" min="0.001" step="0.001" value={form.quantity} onChange={(e) => setForm({ ...form, quantity: e.target.value })} />
            </Field>
            <div className="flex items-end"><Button type="button" variant="ghost" onClick={addSaleItem}>Adicionar item</Button></div>
          </div>
          {saleItems.length > 0 && (
            <div className="rounded-md border border-slate-200">
              {saleItems.map((item, index) => (
                <div key={`${item.productId}-${index}`} className="grid gap-2 border-b border-slate-100 px-3 py-2 text-sm last:border-b-0 md:grid-cols-5">
                  <span className="font-semibold">{item.productName}</span>
                  <span>Estoque: {item.stockBefore}</span>
                  <span>Quantidade: {item.quantity}</span>
                  <span>Unitario: {money.format(item.unitPrice)}</span>
                  <span className="font-semibold">Total: {money.format(item.totalPrice)}</span>
                </div>
              ))}
            </div>
          )}
          <Button disabled={busy}>{busy ? "Registrando..." : "Registrar venda"}</Button>
        </form>
      </Card>
      <Table headers={["Cliente", "Total", "Pagamento", "Status", "Data", "Acoes"]} empty={!sales.length} emptyTitle="Nenhuma venda registrada ainda" emptyText="Crie uma venda para comecar.">
        {sales.map((sale) => (
          <tr key={sale.id}>
            <td className="font-semibold">{sale.customerName ?? "Avulso"}</td>
            <td>{money.format(sale.totalAmount)}</td>
            <td>{sale.paymentMethod}</td>
            <td><StatusPill value={sale.status} /></td>
            <td>{new Date(sale.createdAt).toLocaleString("pt-BR")}</td>
            <td className="flex flex-wrap gap-2">
              <Button variant="ghost" disabled={sale.status !== "OPEN"} onClick={() => action(sale, "pay")}>Marcar paga</Button>
              <Button variant="danger" disabled={sale.status === "CANCELED"} onClick={() => action(sale, "cancel")}>Cancelar</Button>
            </td>
          </tr>
        ))}
      </Table>
    </Page>
  );
}

export function CompanyCustomersPage() {
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [search, setSearch] = useState("");
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [editingId, setEditingId] = useState<number | null>(null);
  const [form, setForm] = useState({ name: "", phone: "", email: "", document: "" });

  const load = () => api.get<Customer[]>("/api/app/customers").then((r) => setCustomers(r.data)).catch((err) => setError(apiErrorMessage(err)));
  useEffect(() => { load(); }, []);

  const filtered = customers.filter((customer) => `${customer.name} ${customer.phone ?? ""}`.toLowerCase().includes(search.toLowerCase()));

  async function submit(event: FormEvent) {
    event.preventDefault();
    setError("");
    setSuccess("");
    try {
      if (editingId) {
        await api.put(`/api/app/customers/${editingId}`, form);
        setSuccess("Cliente atualizado.");
      } else {
        await api.post("/api/app/customers", form);
        setSuccess("Cliente criado.");
      }
      reset();
      load();
    } catch (err) {
      setError(apiErrorMessage(err));
    }
  }

  function edit(customer: Customer) {
    setEditingId(customer.id);
    setForm({ name: customer.name, phone: customer.phone ?? "", email: customer.email ?? "", document: customer.document ?? "" });
  }

  function reset() {
    setEditingId(null);
    setForm({ name: "", phone: "", email: "", document: "" });
  }

  async function remove(customer: Customer) {
    if (!window.confirm(`Deseja desativar o cliente ${customer.name}? Ele nao sera apagado do historico.`)) return;
    try {
      await api.delete(`/api/app/customers/${customer.id}`);
      setSuccess("Cliente desativado.");
      load();
    } catch (err) {
      setError(apiErrorMessage(err));
    }
  }

  return (
    <Page title="Clientes" error={error} success={success}>
      <Card>
        <form onSubmit={submit} className="grid gap-3 lg:grid-cols-5">
          <Field label="Nome" help="Nome do cliente.">
            <TextInput value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} required />
          </Field>
          <Field label="Telefone" help="Opcional.">
            <TextInput value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} />
          </Field>
          <Field label="Email" help="Opcional.">
            <TextInput type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} />
          </Field>
          <Field label="Documento" help="CPF, CNPJ ou identificacao opcional.">
            <TextInput value={form.document} onChange={(e) => setForm({ ...form, document: e.target.value })} />
          </Field>
          <div className="flex items-end"><Button>{editingId ? "Salvar" : "Adicionar"}</Button></div>
        </form>
        {editingId && <Button className="mt-3" type="button" variant="ghost" onClick={reset}>Cancelar edicao</Button>}
      </Card>
      <Card><TextInput placeholder="Buscar por nome ou telefone" value={search} onChange={(e) => setSearch(e.target.value)} /></Card>
      <Table headers={["Nome", "Telefone", "Email", "Documento", "Status", "Acoes"]} empty={!filtered.length} emptyTitle="Nenhum cliente cadastrado" emptyText="Cadastre clientes para vincular vendas quando precisar.">
        {filtered.map((customer) => (
          <tr key={customer.id}>
            <td className="font-semibold">{customer.name}</td><td>{customer.phone ?? "-"}</td><td>{customer.email ?? "-"}</td><td>{customer.document ?? "-"}</td>
            <td><StatusPill value={customer.active ? "ACTIVE" : "INACTIVE"} /></td>
            <td className="flex gap-2"><Button variant="ghost" onClick={() => edit(customer)}>Editar</Button><Button variant="danger" disabled={!customer.active} onClick={() => remove(customer)}>Desativar</Button></td>
          </tr>
        ))}
      </Table>
    </Page>
  );
}

export function CompanyReportsPage() {
  const [summary, setSummary] = useState<ReportSummary | null>(null);
  const [error, setError] = useState("");
  useEffect(() => {
    api.get<ReportSummary>("/api/app/reports/summary").then((r) => setSummary(r.data)).catch((err) => setError(apiErrorMessage(err)));
  }, []);
  return (
    <Page title="Relatorios" error={error}>
      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        <StatCard label="Vendas hoje" value={summary?.dashboard.todaySalesCount ?? "-"} />
        <StatCard label="Faturamento hoje" value={summary ? money.format(summary.dashboard.todayRevenue) : "-"} tone="green" />
        <StatCard label="Produtos baixo estoque" value={summary?.dashboard.lowStockProductsCount ?? "-"} tone="red" />
        <StatCard label="Producoes recentes" value={summary?.dashboard.recentProductionsCount ?? "-"} />
      </div>
      <div className="grid gap-5 xl:grid-cols-2">
        <Table headers={["Produto", "Atual", "Minimo"]} empty={!summary?.lowStockProducts.length} emptyTitle="Nenhum produto com estoque baixo" emptyText="Quando algum produto ficar abaixo do minimo, ele aparecera aqui.">
          {summary?.lowStockProducts.map((product) => <tr key={product.id}><td className="font-semibold">{product.name}</td><td>{product.currentStock}</td><td>{product.minStock}</td></tr>)}
        </Table>
        <Table headers={["Produto", "Quantidade vendida"]} empty={!summary?.topProducts.length} emptyTitle="Sem produtos vendidos ainda" emptyText="Os produtos mais vendidos aparecerao depois das primeiras vendas pagas.">
          {summary?.topProducts.map((row) => <tr key={row.productName}><td className="font-semibold">{row.productName}</td><td>{row.quantity}</td></tr>)}
        </Table>
      </div>
      <Card>
        <SectionTitle title="Movimentacoes recentes" />
        {!summary?.recentStockMovements.length ? <EmptyState title="Sem movimentacoes" text="Movimentacoes recentes aparecerao aqui." /> : <SimpleMovementsTable rows={summary.recentStockMovements} />}
      </Card>
    </Page>
  );
}

type ProductForm = ReturnType<typeof emptyProductForm>;
type SaleDraftItem = { productId: number; productName: string; quantity: number; unitPrice: number; totalPrice: number; stockBefore: number; stockAfter: number };

function emptyProductForm() {
  return { name: "", description: "", sku: "", category: "", unit: "UNIDADE", costPrice: "0", salePrice: "0", minStock: "0", currentStock: "0", active: "true" };
}

function validateProductForm(form: ProductForm) {
  if (!form.name.trim()) return "Informe o nome do produto.";
  if (Number(form.costPrice) < 0) return "O preco de custo nao pode ser negativo.";
  if (Number(form.salePrice) < 0) return "O preco de venda nao pode ser negativo.";
  if (Number(form.minStock) < 0) return "O estoque minimo nao pode ser menor que zero.";
  if (Number(form.currentStock) < 0) return "O estoque atual nao pode ser menor que zero.";
  return "";
}

function getProductWarnings(form: ProductForm) {
  const cost = Number(form.costPrice);
  const sale = Number(form.salePrice);
  const minStock = Number(form.minStock);
  const currentStock = Number(form.currentStock);
  return {
    priceBelowCost: sale > 0 && cost > 0 && sale < cost,
    lowStock: currentStock <= minStock,
  };
}

function numericProduct(form: ProductForm) {
  return {
    ...form,
    costPrice: Number(form.costPrice),
    salePrice: Number(form.salePrice),
    minStock: Number(form.minStock),
    currentStock: Number(form.currentStock),
    active: form.active === "true",
  };
}

function calculateStockAfter(currentStock: number, type: string, quantity: number) {
  if (!quantity || quantity < 0) return null;
  if (type === "IN") return currentStock + quantity;
  if (type === "OUT") return currentStock - quantity;
  if (type === "ADJUSTMENT") return quantity;
  return currentStock;
}

function movementLabel(type: string) {
  if (type === "IN") return "Entrada";
  if (type === "OUT") return "Saida";
  if (type === "ADJUSTMENT") return "Ajuste";
  if (type === "PRODUCTION") return "Producao";
  if (type === "SALE") return "Venda";
  return type;
}

function Page({ title, children, error, success }: { title: string; children: ReactNode; error?: string; success?: string }) {
  return (
    <div className="space-y-5">
      <h1 className="text-2xl font-bold text-ink">{title}</h1>
      {error && <div className="rounded-md bg-red-50 px-4 py-3 text-sm font-semibold text-red-700">{error}</div>}
      {success && <div className="rounded-md bg-emerald-50 px-4 py-3 text-sm font-semibold text-emerald-700">{success}</div>}
      {children}
    </div>
  );
}

function SectionTitle({ title }: { title: string }) {
  return <h2 className="mb-4 text-lg font-bold text-ink">{title}</h2>;
}

function FormSection({ title, children }: { title: string; children: ReactNode }) {
  return (
    <div>
      <h3 className="mb-3 text-sm font-bold uppercase text-slate-500">{title}</h3>
      <div className="grid gap-3 lg:grid-cols-2 xl:grid-cols-3">{children}</div>
    </div>
  );
}

function Field({ label, help, children }: { label: string; help: string; children: ReactNode }) {
  return (
    <label className="block">
      <span className="mb-1 block text-sm font-semibold text-ink">{label}</span>
      {children}
      <span className="mt-1 block text-xs leading-5 text-slate-500">{help}</span>
    </label>
  );
}

function InfoBox({ children }: { children: ReactNode }) {
  return <div className="rounded-md border border-blue-100 bg-blue-50 px-4 py-3 text-sm font-medium leading-6 text-blue-800">{children}</div>;
}

function SummaryBox({ children, className = "" }: { children: ReactNode; className?: string }) {
  return <div className={`rounded-md border border-slate-200 bg-slate-50 px-4 py-3 text-sm leading-6 text-slate-700 ${className}`}>{children}</div>;
}

function ProductSummary({ form, warnings }: { form: ProductForm; warnings: ReturnType<typeof getProductWarnings> }) {
  return (
    <SummaryBox>
      <div className="font-bold text-ink">Resumo antes de salvar</div>
      <div>Produto: {form.name || "-"}</div>
      <div>Preco de venda: {money.format(Number(form.salePrice || 0))}</div>
      <div>Estoque atual: {form.currentStock || "0"} {form.unit}</div>
      {warnings.priceBelowCost && <div className="mt-2 font-semibold text-danger">Atencao: o preco de venda esta menor que o preco de custo.</div>}
      {warnings.lowStock && <div className="font-semibold text-amber-700">Atencao: o estoque atual esta abaixo ou igual ao estoque minimo.</div>}
    </SummaryBox>
  );
}

function Table({ headers, children, empty, emptyTitle = "Nenhum registro encontrado", emptyText = "Os dados aparecerao aqui quando forem cadastrados." }: { headers: string[]; children: ReactNode; empty?: boolean; emptyTitle?: string; emptyText?: string }) {
  return (
    <Card className="overflow-x-auto p-0">
      {empty ? (
        <div className="p-5"><EmptyState title={emptyTitle} text={emptyText} /></div>
      ) : (
        <table className="w-full min-w-[760px] text-left text-sm">
          <thead className="bg-slate-50 text-xs uppercase text-slate-500">
            <tr>{headers.map((header) => <th key={header} className="px-4 py-3 font-bold">{header}</th>)}</tr>
          </thead>
          <tbody className="divide-y divide-slate-100">{children}</tbody>
        </table>
      )}
    </Card>
  );
}

function SimpleMovementsTable({ rows }: { rows: StockMovement[] }) {
  return (
    <div className="overflow-x-auto">
      <table className="w-full min-w-[720px] text-left text-sm">
        <thead className="bg-slate-50 text-xs uppercase text-slate-500">
          <tr><th className="px-4 py-3">Produto</th><th className="px-4 py-3">Tipo</th><th className="px-4 py-3">Qtd.</th><th className="px-4 py-3">Motivo</th><th className="px-4 py-3">Usuario</th><th className="px-4 py-3">Data</th></tr>
        </thead>
        <tbody className="divide-y divide-slate-100">
          {rows.map((row) => <tr key={row.id}><td className="font-semibold">{row.productName}</td><td>{movementLabel(row.type)}</td><td>{row.quantity}</td><td>{row.reason ?? "-"}</td><td>{row.createdBy}</td><td>{new Date(row.createdAt).toLocaleString("pt-BR")}</td></tr>)}
        </tbody>
      </table>
    </div>
  );
}
