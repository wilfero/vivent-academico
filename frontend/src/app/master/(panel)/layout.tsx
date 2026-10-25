import { PlatformShell } from "@/features/platform/components/platform-shell";

export default function MasterPanelLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return <PlatformShell>{children}</PlatformShell>;
}
