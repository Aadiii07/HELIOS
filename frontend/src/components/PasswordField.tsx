import { useState } from "react";

interface PasswordFieldProps {
  id: string;
  value: string;
  onChange: (value: string) => void;
  autoComplete: "current-password" | "new-password";
}

export function PasswordField({ id, value, onChange, autoComplete }: PasswordFieldProps) {
  const [visible, setVisible] = useState(false);

  return (
    <div className="password-field-wrapper">
      <input
        id={id}
        type={visible ? "text" : "password"}
        autoComplete={autoComplete}
        required
        value={value}
        onChange={(e) => onChange(e.target.value)}
      />
      <button
        type="button"
        className="password-toggle"
        onClick={() => setVisible((v) => !v)}
        aria-label={visible ? "Hide password" : "Show password"}
        aria-pressed={visible}
      >
        {visible ? "Hide" : "Show"}
      </button>
    </div>
  );
}
