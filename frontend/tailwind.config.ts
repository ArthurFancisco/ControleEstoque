import type { Config } from "tailwindcss";

export default {
  content: ["./index.html", "./src/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        ink: "#102033",
        brand: "#1557a6",
        mint: "#168060",
        danger: "#d83a3a",
        surface: "#f5f7fa"
      },
      boxShadow: {
        soft: "0 12px 32px rgba(16, 32, 51, 0.08)"
      }
    }
  },
  plugins: []
} satisfies Config;
