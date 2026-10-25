import { getStoredSession } from "@/features/auth/lib/session-storage";

export type BranchOption = { value: string; label: string };

export function getSessionBranchOption(): BranchOption | null {
  const session = getStoredSession();

  if (!session?.branchId) {
    return null;
  }

  return { value: session.branchId, label: session.branchName ?? session.branchId };
}

export function withSessionBranchFallback(options: BranchOption[]) {
  if (options.length > 0) {
    return options;
  }

  const sessionBranch = getSessionBranchOption();
  return sessionBranch ? [sessionBranch] : [];
}
