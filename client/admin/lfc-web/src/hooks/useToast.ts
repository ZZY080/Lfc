import { createContext, useContext } from 'react'

type ToastType = 'success' | 'error' | 'info'

export interface ToastContextValue {
  show: (message: string, type?: ToastType) => void
  success: (message: string) => void
  error: (message: string) => void
}

export const ToastContext = createContext<ToastContextValue | null>(null)

export function useToast() {
  const context = useContext(ToastContext)
  if (!context) {
    throw new Error('useToast must be used within ToastProvider')
  }
  return context
}
