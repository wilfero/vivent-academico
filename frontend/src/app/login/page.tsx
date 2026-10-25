import { Suspense } from "react";
import { PublicShell } from "@/components/layout/public-shell";
import { LoginForm } from "@/features/auth/components/login-form";

export default function LoginPage() {
  return (
    <PublicShell>
      <Suspense fallback={<p className="text-sm text-muted-foreground">Cargando acceso...</p>}>
        <LoginForm />
      </Suspense>
    </PublicShell>
  );
}
