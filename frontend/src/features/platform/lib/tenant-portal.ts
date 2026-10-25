export function buildTenantPortalUrl(domain: string) {
  return `https://${domain}/login`;
}

export function formatTenantCredentials(domain: string, username: string, password: string) {
  return [`Portal: ${buildTenantPortalUrl(domain)}`, `Usuario: ${username}`, `Contraseña: ${password}`].join("\n");
}

export async function copyToClipboard(value: string) {
  if (!navigator.clipboard) {
    throw new Error("Clipboard API is not available");
  }

  await navigator.clipboard.writeText(value);
}
