import * as React from "react";
import { cn } from "@/lib/utils";

export function Label({ className, ...props }: React.LabelHTMLAttributes<HTMLLabelElement>) {
  return <label className={cn("text-xs font-bold uppercase leading-none tracking-[0.16em] text-muted-foreground", className)} {...props} />;
}
