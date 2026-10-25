"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import { Eye, EyeOff } from "lucide-react";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { toast } from "sonner";
import { z } from "zod";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { platformLogin } from "@/features/platform/lib/platform-api";
import { ApiError } from "@/lib/api/errors";

const loginSchema = z.object({
  username: z.string().min(1, "El usuario es obligatorio"),
  password: z.string().min(1, "La contraseña es obligatoria"),
});

type LoginFormData = z.infer<typeof loginSchema>;

export function PlatformLoginForm() {
  const router = useRouter();
  const [showPassword, setShowPassword] = useState(false);

  const form = useForm<LoginFormData>({
    resolver: zodResolver(loginSchema),
    defaultValues: { username: "", password: "" },
  });

  const loginMutation = useMutation({
    mutationFn: (values: LoginFormData) => platformLogin(values.username, values.password),
    onSuccess: () => router.replace("/master/companies"),
    onError: (error) => {
      if (error instanceof ApiError && error.errorCode === "INVALID_CREDENTIALS") {
        toast.error("Usuario o contraseña incorrectos");
        return;
      }

      if (error instanceof ApiError && error.errorCode === "PLATFORM_ACCOUNT_LOCKED") {
        toast.error("Cuenta bloqueada temporalmente por intentos fallidos");
        return;
      }

      toast.error(error instanceof Error ? error.message : "No se pudo iniciar sesión");
    },
  });

  return (
    <Card className="w-full max-w-md border-border bg-card/90">
      <CardHeader>
        <div className="mb-2 inline-flex w-fit items-center rounded-full border border-primary/20 bg-primary/10 px-3 py-1 text-xs font-bold uppercase tracking-[0.18em] text-primary">
          Panel de administración
        </div>
        <CardTitle>Vivent Master</CardTitle>
        <CardDescription>Administración de empresas, planes y suscripciones de la plataforma.</CardDescription>
      </CardHeader>
      <CardContent>
        <form className="space-y-4" onSubmit={form.handleSubmit((values) => loginMutation.mutate(values))}>
          <div className="space-y-2">
            <Label htmlFor="username">Usuario</Label>
            <Input id="username" autoComplete="username" disabled={loginMutation.isPending} {...form.register("username")} />
          </div>
          <div className="space-y-2">
            <Label htmlFor="password">Contraseña</Label>
            <div className="relative">
              <Input
                id="password"
                type={showPassword ? "text" : "password"}
                autoComplete="current-password"
                disabled={loginMutation.isPending}
                className="pr-10"
                {...form.register("password")}
              />
              <button
                type="button"
                className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
                onClick={() => setShowPassword((value) => !value)}
                tabIndex={-1}
                aria-label={showPassword ? "Ocultar contraseña" : "Mostrar contraseña"}
              >
                {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
              </button>
            </div>
          </div>
          <Button className="w-full" type="submit" disabled={loginMutation.isPending}>
            {loginMutation.isPending ? "Validando..." : "Ingresar"}
          </Button>
        </form>
      </CardContent>
    </Card>
  );
}
