import { FormEvent, useState } from "react";
import { BriefcaseBusiness, LockKeyhole, Mail, UserRound } from "lucide-react";
import { BrandMark } from "../../components/BrandMark";
import { ThemeToggle } from "../../components/ThemeToggle";
import { Button } from "../../components/ui/Button";
import { Card } from "../../components/ui/Card";
import { Input } from "../../components/ui/Input";
import { PwaInstallPrompt } from "../../components/PwaInstallPrompt";
import { loginAdmin, loginEmployee, type AppSettings, type AuthResponse } from "../../lib/api";
import venueUrl from "../../assets/brand/breta-palace-wide.jpg";

type LoginPageProps = {
  settings: AppSettings | null;
  onAuthenticated: (session: AuthResponse) => void;
};

export function LoginPage({ settings, onAuthenticated }: LoginPageProps) {
  const [mode, setMode] = useState<"employee" | "admin">("employee");
  const [employeeUsername, setEmployeeUsername] = useState("");
  const [employeePassword, setEmployeePassword] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    setLoading(true);

    try {
      const session =
        mode === "employee" ? await loginEmployee(employeeUsername, employeePassword) : await loginAdmin(email, password);
      onAuthenticated(session);
    } catch (error) {
      setError(error instanceof Error ? error.message : "Login failed. Please check your details and try again.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="grid min-h-screen bg-background lg:grid-cols-[1.1fr_0.9fr]">
      <section className="relative hidden overflow-hidden lg:block">
        <img src={venueUrl} alt="" className="h-full w-full object-cover" />
        <div className="absolute inset-0 bg-black/35" />
        <div className="absolute bottom-10 left-10 max-w-xl text-white">
          <p className="text-sm font-semibold uppercase tracking-[0.24em] text-primary">Workforce operations</p>
          <h1 className="mt-4 font-display text-5xl leading-tight">Art Decor Events</h1>
          <p className="mt-4 max-w-md text-base text-white/82">
            Attendance and daily coordination for premium decoration teams.
          </p>
        </div>
      </section>

      <section className="brand-surface flex min-h-screen items-center justify-center px-5 py-8 pb-[calc(2rem+env(safe-area-inset-bottom))] pt-[calc(2rem+env(safe-area-inset-top))]">
        <div className="absolute right-5 top-5">
          <ThemeToggle />
        </div>
        <Card className="w-full max-w-md p-5 shadow-corporate sm:p-6">
          <BrandMark logoUrl={settings?.logoUrl} companyName={settings?.companyName} />

          <div className="mt-8 grid grid-cols-2 rounded-lg bg-muted p-1">
            <Button
              type="button"
              variant={mode === "employee" ? "primary" : "ghost"}
              onClick={() => setMode("employee")}
              className="h-10"
            >
              <UserRound size={18} />
              Employee
            </Button>
            <Button
              type="button"
              variant={mode === "admin" ? "primary" : "ghost"}
              onClick={() => setMode("admin")}
              className="h-10"
            >
              <BriefcaseBusiness size={18} />
              Admin
            </Button>
          </div>

          <form className="mt-6 space-y-4" onSubmit={submit}>
            {mode === "employee" ? (
              <>
                <label className="block space-y-2">
                  <span className="text-sm font-medium">Employee ID</span>
                  <Input value={employeeUsername} onChange={(event) => setEmployeeUsername(event.target.value)} required />
                </label>
                <label className="block space-y-2">
                  <span className="text-sm font-medium">Password</span>
                  <div className="relative">
                    <LockKeyhole className="absolute left-3 top-3 text-muted-foreground" size={18} />
                    <Input
                      value={employeePassword}
                      onChange={(event) => setEmployeePassword(event.target.value)}
                      className="pl-10"
                      type="password"
                      required
                    />
                  </div>
                </label>
              </>
            ) : (
              <>
                <label className="block space-y-2">
                  <span className="text-sm font-medium">Email</span>
                  <div className="relative">
                    <Mail className="absolute left-3 top-3 text-muted-foreground" size={18} />
                    <Input
                      value={email}
                      onChange={(event) => setEmail(event.target.value)}
                      className="pl-10"
                      type="email"
                      required
                    />
                  </div>
                </label>
                <label className="block space-y-2">
                  <span className="text-sm font-medium">Password</span>
                  <div className="relative">
                    <LockKeyhole className="absolute left-3 top-3 text-muted-foreground" size={18} />
                    <Input
                      value={password}
                      onChange={(event) => setPassword(event.target.value)}
                      className="pl-10"
                      type="password"
                      required
                    />
                  </div>
                </label>
              </>
            )}

            {error && <p className="rounded-md bg-destructive/10 px-3 py-2 text-sm text-destructive">{error}</p>}

            <Button className="w-full" disabled={loading}>
              {loading ? "Signing in..." : mode === "employee" ? "Open my shift" : "Open dashboard"}
            </Button>
          </form>
          <div className="mt-4">
            <PwaInstallPrompt />
          </div>
        </Card>
      </section>
    </main>
  );
}
