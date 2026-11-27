import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { QueryClientProvider } from '@tanstack/react-query'
import { queryClient } from './lib/queryClient'
import './index.css'
import App from './App.tsx'
import { AuthSessionGate } from './components/routing/AuthSessionGate'
import { ConnectionStatusPopup } from './components/common/ConnectionStatusPopup'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <ConnectionStatusPopup />
      <AuthSessionGate><App /></AuthSessionGate>
    </QueryClientProvider>
  </StrictMode>,
)

