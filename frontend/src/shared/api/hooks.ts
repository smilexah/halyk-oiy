import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useAuth } from '../../features/auth/AuthProvider'
import { budgetApi, familyApi, goalsApi, offersApi, transactionsApi } from './endpoints'
import type { ContributeRequest, CreateGoalRequest } from './types'

/**
 * Queries are gated on an authenticated session — the gateway rejects every
 * /api call without a JWT, so until Keycloak login is enabled they stay idle
 * and screens fall back to demo data (no 401 noise). Flip VITE_AUTH_ENABLED
 * and log in → the same hooks go live with zero component changes.
 */
function useLiveEnabled(): boolean {
  const { enabled, authenticated } = useAuth()
  return enabled && authenticated
}

export const qk = {
  dashboard: ['budget', 'dashboard'] as const,
  transactions: ['transactions'] as const,
  goals: ['goals'] as const,
  myGroups: ['family', 'groups'] as const,
  group: (id: string) => ['family', 'group', id] as const,
  offers: ['offers'] as const,
}

export function useDashboard() {
  return useQuery({ queryKey: qk.dashboard, queryFn: budgetApi.dashboard, enabled: useLiveEnabled() })
}

export function useTransactions() {
  return useQuery({ queryKey: qk.transactions, queryFn: transactionsApi.list, enabled: useLiveEnabled() })
}

export function useGoals() {
  return useQuery({ queryKey: qk.goals, queryFn: goalsApi.list, enabled: useLiveEnabled() })
}

export function useMyGroups() {
  return useQuery({ queryKey: qk.myGroups, queryFn: familyApi.myGroups, enabled: useLiveEnabled() })
}

export function useGroup(id: string | null | undefined) {
  return useQuery({
    queryKey: qk.group(id ?? ''),
    queryFn: () => familyApi.group(id as string),
    enabled: useLiveEnabled() && !!id,
  })
}

export function useOffers() {
  return useQuery({ queryKey: qk.offers, queryFn: offersApi.list, enabled: useLiveEnabled() })
}

/* ── mutations (user-triggered; invalidate on success) ───────── */
export function useCreateGoal() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (body: CreateGoalRequest) => goalsApi.create(body),
    onSuccess: () => qc.invalidateQueries({ queryKey: qk.goals }),
  })
}

export function useContribute(goalId: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (body: ContributeRequest) => goalsApi.contribute(goalId, body),
    onSuccess: () => qc.invalidateQueries({ queryKey: qk.goals }),
  })
}
