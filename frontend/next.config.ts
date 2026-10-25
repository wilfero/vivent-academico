import type { NextConfig } from "next";
import createNextIntlPlugin from "next-intl/plugin";

const nextConfig: NextConfig = {
  allowedDevOrigins: ["192.168.50.94"],
  output: "standalone",
  // Development-only proxy for the master panel API. In production the central
  // nginx routes /api/v1/platform/** to the backend, so no rewrite is applied.
  async rewrites() {
    if (process.env.NODE_ENV !== "development") {
      return [];
    }

    const target = process.env.PLATFORM_DEV_PROXY_TARGET ?? "http://localhost:8080";

    return [
      {
        source: "/api/v1/platform/:path*",
        destination: `${target}/api/v1/platform/:path*`,
      },
    ];
  },
};

const withNextIntl = createNextIntlPlugin("./src/lib/i18n/request.ts");

export default withNextIntl(nextConfig);
