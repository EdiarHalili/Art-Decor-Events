import logoUrl from "../assets/brand/art-decor-logo.jpg";
import { cn } from "../lib/utils";

type BrandMarkProps = {
  compact?: boolean;
  className?: string;
  logoUrl?: string | null;
  companyName?: string;
};

export function BrandMark({ compact = false, className, logoUrl: managedLogoUrl, companyName = "Art Decor Events" }: BrandMarkProps) {
  const title = companyName.replace(/\s+Events$/i, "");
  return (
    <div className={cn("flex min-w-0 items-center gap-3", className)}>
      <img
        src={managedLogoUrl || logoUrl}
        alt={companyName}
        className={cn("rounded-full border border-primary/30 object-cover", compact ? "h-10 w-10" : "h-14 w-14")}
      />
      {!compact && (
        <div className="min-w-0">
          <p className="text-sm font-semibold uppercase tracking-[0.12em] text-primary sm:tracking-[0.18em]">{title}</p>
          <p className="text-xs text-muted-foreground">Events Workforce</p>
        </div>
      )}
    </div>
  );
}
