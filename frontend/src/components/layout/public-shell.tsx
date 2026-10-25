import { PreferenceControls } from "@/components/layout/preference-controls";

export function PublicShell({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <main className="min-h-screen bg-background px-4 py-6 sm:px-6 lg:px-8">
      <div className="fixed right-4 top-4 z-20 sm:right-6 sm:top-6">
        <PreferenceControls />
      </div>
      <div className="mx-auto flex min-h-[calc(100vh-3rem)] w-full max-w-7xl items-center justify-center">
        {children}
      </div>
    </main>
  );
}
