import { api } from './client'
import type {
  AddMemberRequest,
  ApprovalRequest,
  ApprovalResponse,
  ChildLimitResponse,
  ContributeRequest,
  CreateGoalRequest,
  CreateGroupRequest,
  CreatePlanRequest,
  DashboardResponse,
  GoalResponse,
  GroupResponse,
  InviteRequest,
  InviteResponse,
  Offer,
  SetLimitRequest,
  TransactionRequest,
  TransactionResponse,
} from './types'

/** Typed wrappers over the gateway REST API. One function per backend endpoint. */

export const budgetApi = {
  dashboard: () => api.get<DashboardResponse>('/budget/dashboard'),
  createPlan: (body: CreatePlanRequest) => api.post<string>('/budget/plan', body),
}

export const transactionsApi = {
  list: () => api.get<TransactionResponse[]>('/transactions'),
  create: (body: TransactionRequest) => api.post<TransactionResponse>('/transactions', body),
}

export const goalsApi = {
  list: () => api.get<GoalResponse[]>('/goals'),
  create: (body: CreateGoalRequest) => api.post<GoalResponse>('/goals', body),
  contribute: (id: string, body: ContributeRequest) =>
    api.post<GoalResponse>(`/goals/${id}/contribute`, body),
}

export const familyApi = {
  group: (id: string) => api.get<GroupResponse>(`/family/groups/${id}`),
  createGroup: (body: CreateGroupRequest) => api.post<GroupResponse>('/family/groups', body),
  addMember: (groupId: string, body: AddMemberRequest) =>
    api.post<GroupResponse>(`/family/groups/${groupId}/members`, body),
  setLimit: (membershipId: string, body: SetLimitRequest) =>
    api.put<GroupResponse>(`/family/members/${membershipId}/limit`, body),
  childLimit: (userId: string) =>
    api.get<ChildLimitResponse>(`/family/members/by-user/${userId}/limit`),
  approve: (transactionId: string, body: ApprovalRequest) =>
    api.post<ApprovalResponse>(`/family/approvals/${transactionId}`, body),
}

export const authApi = {
  invite: (body: InviteRequest) => api.post<InviteResponse>('/auth/invite', body),
}

export const offersApi = {
  list: () => api.get<Offer[]>('/integration/offers'),
}
