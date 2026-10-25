import * as React from "react";
import { cn } from "@/lib/utils";

export type ButtonProps = React.ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: "default" | "secondary" | "ghost" | "outline" | "panel";
};

export function Button({ className, variant = "default", ...props }: ButtonProps) {
  const variants = {
    default: "scanline bg-primary text-primary-foreground shadow-lg shadow-primary/20 hover:bg-primary/92",
    secondary: "bg-secondary text-secondary-foreground hover:bg-secondary/80",
    ghost: "hover:bg-accent hover:text-accent-foreground",
    outline: "border border-input bg-background/70 hover:bg-accent hover:text-accent-foreground",
    panel: "bg-panel text-panel-foreground hover:bg-panel/92",
  };

  return (
    <button
      className={cn(
        "inline-flex h-10 items-center justify-center rounded-lg px-4 py-2 text-sm font-semibold tracking-[-0.01em] transition-all duration-200 hover:-translate-y-0.5 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:pointer-events-none disabled:translate-y-0 disabled:opacity-50",
        variants[variant],
        className,
      )}
      {...props}
    />
  );
}
