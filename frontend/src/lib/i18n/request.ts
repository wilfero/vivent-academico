import { getRequestConfig } from "next-intl/server";
import { defaultLocale, getMessages } from "@/lib/i18n/messages";

export default getRequestConfig(async () => ({
  locale: defaultLocale,
  messages: getMessages(defaultLocale),
  timeZone: "America/Guatemala",
}));
