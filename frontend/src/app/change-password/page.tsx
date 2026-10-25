import { PublicShell } from "@/components/layout/public-shell";
import { RequiredPasswordChangeScreen } from "@/features/account/required-password-change-screen";

export default function ChangePasswordPage() {
  return (
    <PublicShell>
      <RequiredPasswordChangeScreen />
    </PublicShell>
  );
}
