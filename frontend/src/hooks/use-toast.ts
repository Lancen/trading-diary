import { create } from "zustand";

export interface Toast {
  id: string;
  message: string;
  type: "success" | "error";
}

interface ToastStore {
  toasts: Toast[];
  toast: (message: string, type: "success" | "error") => void;
  dismiss: (id: string) => void;
}

let nextId = 0;

export const useToast = create<ToastStore>((set) => ({
  toasts: [],
  toast: (message, type) => {
    const id = String(++nextId);
    set((s) => ({ toasts: [...s.toasts, { id, message, type }] }));
    setTimeout(() => {
      set((s) => ({ toasts: s.toasts.filter((t) => t.id !== id) }));
    }, 4000);
  },
  dismiss: (id) => {
    set((s) => ({ toasts: s.toasts.filter((t) => t.id !== id) }));
  },
}));
