import esGT from "@/messages/es-GT.json";
import enUS from "@/messages/en-US.json";

export const defaultLocale = "es-GT" as const;
export const locales = ["es-GT", "en-US"] as const;

export type AppLocale = (typeof locales)[number];

export function getDefaultMessages() {
  return esGT;
}

export function getMessages(locale: string) {
  return locale === "en-US" ? enUS : esGT;
}

export const clientMessages = {
  "es-GT": esGT,
  "en-US": enUS,
} as const;
