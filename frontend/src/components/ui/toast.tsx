"use client";

import { useToast } from "@/hooks/use-toast";

export function Toaster() {
  const { toasts, dismiss } = useToast();

  return (
    <div className="pointer-events-none fixed bottom-4 right-4 z-50 flex flex-col gap-2">
      {toasts.map((t) => (
        <div
          key={t.id}
          onClick={() => dismiss(t.id)}
          className={
            "pointer-events-auto cursor-pointer rounded-lg px-4 py-3 text-sm shadow-lg transition-opacity " +
            (t.type === "success"
              ? "bg-green-600 text-white"
              : "bg-red-600 text-white")
          }
        >
          {t.message}
        </div>
      ))}
    </div>
  );
}
