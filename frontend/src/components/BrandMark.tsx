import logoUrl from "../assets/brand/art-decor-logo.jpg";
import { cn } from "../lib/utils";

type BrandMarkProps = {
  compact?: boolean;
  className?: string;
};

export function BrandMark({ compact = false, className }: BrandMarkProps) {
  return (
    <div className={cn("flex items-center gap-3", className)}>
      <img
        src={logoUrl}
        alt="Art Decor Events"
        className={cn("rounded-full border border-primary/30 object-cover", compact ? "h-10 w-10" : "h-14 w-14")}
      />
      {!compact && (
        <div>
          <p className="text-sm font-semibold uppercase tracking-[0.18em] text-primary">Art Decor</p>
          <p className="text-xs text-muted-foreground">Events Workforce</p>
        </div>
      )}
    </div>
  );
}

