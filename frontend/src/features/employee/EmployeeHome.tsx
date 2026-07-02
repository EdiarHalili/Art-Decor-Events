import { Bell, CalendarClock, LogOut, MapPin, Wifi } from "lucide-react";
import { BrandMark } from "../../components/BrandMark";
import { Button } from "../../components/ui/Button";
import { Card } from "../../components/ui/Card";
import type { AuthResponse } from "../../lib/api";

type EmployeeHomeProps = {
  session: AuthResponse;
  onLogout: () => void;
};

export function EmployeeHome({ session, onLogout }: EmployeeHomeProps) {
  return (
    <main className="brand-surface min-h-screen px-4 py-5">
      <div className="mx-auto flex max-w-md flex-col gap-5">
        <header className="flex items-center justify-between">
          <BrandMark compact />
          <div className="flex items-center gap-2 rounded-full border border-border bg-card px-3 py-2 text-xs text-muted-foreground">
            <Wifi size={15} />
            Online
          </div>
        </header>

        <section>
          <p className="text-sm text-muted-foreground">Welcome</p>
          <h1 className="mt-1 text-2xl font-semibold">{session.fullName}</h1>
        </section>

        <Card className="p-5">
          <div className="flex items-start gap-3">
            <div className="rounded-md bg-primary/15 p-2 text-primary">
              <CalendarClock size={22} />
            </div>
            <div>
              <h2 className="text-lg font-semibold">Today's assignment</h2>
              <p className="mt-1 text-sm text-muted-foreground">No assignment published for today.</p>
            </div>
          </div>

          <div className="mt-5 rounded-md bg-muted p-4 text-sm">
            <div className="flex items-center gap-2 font-medium">
              <MapPin size={17} />
              Status
            </div>
            <p className="mt-2 text-muted-foreground">Check-in is not open.</p>
          </div>

          <div className="mt-5 grid gap-3">
            <Button className="h-14 text-base">Check In</Button>
            <Button className="h-14 text-base" variant="secondary">
              Check Out
            </Button>
          </div>
        </Card>

        <Card className="p-5">
          <div className="flex items-center gap-3">
            <Bell className="text-primary" size={21} />
            <h2 className="font-semibold">Announcements</h2>
          </div>
          <p className="mt-3 text-sm text-muted-foreground">Welcome to Art Decor Events Workforce.</p>
        </Card>

        <Button variant="ghost" onClick={onLogout}>
          <LogOut size={18} />
          Sign out
        </Button>
      </div>
    </main>
  );
}

