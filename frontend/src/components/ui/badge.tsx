import * as React from "react"
import { cva, type VariantProps } from "class-variance-authority"
import { cn } from "@/lib/utils"

const badgeVariants = cva(
  "inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-xs font-semibold tracking-wide transition-colors focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2 select-none",
  {
    variants: {
      variant: {
        default:
          "border-transparent bg-primary text-primary-foreground hover:bg-primary/80",
        secondary:
          "border-transparent bg-secondary text-secondary-foreground hover:bg-secondary/80",
        destructive:
          "border-transparent bg-destructive/15 text-destructive border border-destructive/20",
        outline: "text-foreground border border-border",
        success:
          "border-transparent bg-emerald-500/15 text-emerald-600 dark:text-emerald-400 border border-emerald-500/25",
        warning:
          "border-transparent bg-amber-500/15 text-amber-600 dark:text-amber-400 border border-amber-500/25",
        info:
          "border-transparent bg-sky-500/15 text-sky-600 dark:text-sky-400 border border-sky-500/25",
        gold:
          "border-transparent bg-gradient-to-r from-amber-500/20 to-yellow-500/20 text-amber-600 dark:text-amber-300 border border-amber-400/30",
        role:
          "border-transparent bg-indigo-500/15 text-indigo-600 dark:text-indigo-400 border border-indigo-500/25 font-bold uppercase text-[10px]",
      },
      size: {
        sm: "px-2 py-0.2 text-[10px]",
        default: "px-2.5 py-0.5 text-xs",
        lg: "px-3 py-1 text-sm",
      }
    },
    defaultVariants: {
      variant: "default",
      size: "default",
    },
  }
)

export interface BadgeProps
  extends React.HTMLAttributes<HTMLDivElement>,
    VariantProps<typeof badgeVariants> {}

function Badge({ className, variant, size, ...props }: BadgeProps) {
  return (
    <div className={cn(badgeVariants({ variant, size }), className)} {...props} />
  )
}

export { Badge, badgeVariants }
