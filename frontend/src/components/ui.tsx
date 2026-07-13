import { clsx } from "clsx";
import type { ReactNode } from "react";

export function Button({ className, variant = "primary", ...props }: React.ButtonHTMLAttributes<HTMLButtonElement> & { variant?: "primary" | "ghost" | "danger" }) {
  return (
    <button
      className={clsx(
        "focus-ring inline-flex min-h-10 items-center justify-center gap-2 rounded-md px-4 text-sm font-semibold transition disabled:cursor-not-allowed disabled:opacity-60",
        variant === "primary" && "bg-brand text-white hover:bg-blue-800",
        variant === "ghost" && "bg-white text-ink ring-1 ring-slate-200 hover:bg-slate-50",
        variant === "danger" && "bg-danger text-white hover:bg-red-700",
        className
      )}
      {...props}
    />
  );
}

export function Card({ children, className }: { children: ReactNode; className?: string }) {
  return <section className={clsx("rounded-lg border border-slate-200 bg-white p-5 shadow-soft", className)}>{children}</section>;
}

export function StatCard({ label, value, tone = "blue" }: { label: string; value: ReactNode; tone?: "blue" | "green" | "red" | "gray" }) {
  return (
    <Card className="min-h-28">
      <div className="text-sm font-medium text-slate-500">{label}</div>
      <div className={clsx("mt-3 text-2xl font-bold", tone === "green" && "text-mint", tone === "red" && "text-danger", tone === "blue" && "text-brand", tone === "gray" && "text-ink")}>{value}</div>
    </Card>
  );
}

export function TextInput(props: React.InputHTMLAttributes<HTMLInputElement>) {
  return <input className="focus-ring h-10 w-full rounded-md border border-slate-200 bg-white px-3 text-sm" {...props} />;
}

export function Select(props: React.SelectHTMLAttributes<HTMLSelectElement>) {
  return <select className="focus-ring h-10 w-full rounded-md border border-slate-200 bg-white px-3 text-sm" {...props} />;
}

export function StatusPill({ value }: { value: string }) {
  const tone =
    value === "ACTIVE" || value === "ONLINE" || value === "PAID" || value === "FINISHED"
      ? "bg-emerald-50 text-emerald-700"
      : value === "TRIAL" || value === "OPEN" || value === "PLANNED" || value === "IN_PROGRESS"
        ? "bg-blue-50 text-blue-700"
        : value === "PAST_DUE"
          ? "bg-amber-50 text-amber-700"
          : value === "CANCELED" || value === "INACTIVE"
            ? "bg-slate-100 text-slate-600"
            : "bg-red-50 text-red-700";
  return <span className={clsx("inline-flex rounded-full px-2.5 py-1 text-xs font-semibold", tone)}>{value}</span>;
}

export function EmptyState({ title, text }: { title: string; text: string }) {
  return (
    <div className="rounded-lg border border-dashed border-slate-300 bg-slate-50 px-4 py-8 text-center">
      <div className="font-semibold text-ink">{title}</div>
      <div className="mt-1 text-sm text-slate-500">{text}</div>
    </div>
  );
}
