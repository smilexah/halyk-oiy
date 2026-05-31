/**
 * Backend DTOs (mirror the Spring records exposed through the gateway under /api).
 * Money is BigDecimal on the server → arrives as number in JSON.
 */

/* ── enums ─────────────────────────────────────────────────── */
export type Role = 'ADULT' | 'CHILD'
export type CategoryType = 'MANDATORY' | 'DISCRETIONARY'
export type OwnerType = 'USER' | 'GROUP'
export type Direction = 'DEBIT' | 'CREDIT'
export type OperationType = 'PURCHASE' | 'TRANSFER' | 'TOPUP' | 'PAYMENT' | 'SALARY' | 'MISC'

/* ── budget ────────────────────────────────────────────────── */
export interface CategoryRequest {
  name: string
  type: CategoryType
  limitAmount: number
}
export interface CreatePlanRequest {
  ownerType?: OwnerType
  ownerId?: string
  periodStart: string // ISO date
  periodEnd: string
  categories: CategoryRequest[]
}
export interface CategoryView {
  name: string
  type: CategoryType
  limit: number
  spent: number
  remaining: number
}
export interface DashboardResponse {
  planId: string
  periodStart: string
  periodEnd: string
  totalLimit: number
  totalSpent: number
  categories: CategoryView[]
}

/* ── transactions ──────────────────────────────────────────── */
export interface TransactionRequest {
  accountId: string
  amount: number
  merchant?: string
  mcc?: string
  occurredAt?: string
  direction?: Direction
  operationType?: OperationType
  currency?: string
  details?: string
  balanceAfter?: number
}
export interface TransactionResponse {
  id: string
  accountId: string
  userId: string
  amount: number
  merchant?: string
  mcc?: string
  categoryName?: string
  occurredAt: string
  status: string
  direction: Direction
  operationType: OperationType
  currency: string
  details?: string
  balanceAfter?: number
}

/* ── goals ─────────────────────────────────────────────────── */
export interface CreateGoalRequest {
  name: string
  description?: string
  category?: string
  targetAmount: number
  monthlyContribution?: number
  deadline?: string
  bonusProgramRef?: string
}
export interface GoalResponse {
  id: string
  name: string
  description?: string
  category?: string
  targetAmount: number
  allocatedAmount: number
  monthlyContribution?: number
  progressPercent: number
  deadline?: string
  virtualAccountBalance?: number
  bonusProgramRef?: string
  createdAt: string
}
export interface ContributeRequest {
  amount: number
}

/* ── family ────────────────────────────────────────────────── */
export interface MemberView {
  membershipId: string
  userId: string
  role: Role
  dailyLimit?: number
}
export interface GroupResponse {
  id: string
  name: string
  type: string
  createdBy: string
  createdAt: string
  members: MemberView[]
}
export interface CreateGroupRequest {
  name: string
}
export interface AddMemberRequest {
  userId: string
  role: Role
  dailyLimit?: number
}
export interface SetLimitRequest {
  dailyLimit: number
}
export interface ChildLimitResponse {
  userId: string
  dailyLimit: number
  overrideUntil?: string
  overrideAmount?: number
}
export interface ApprovalRequest {
  childUserId: string
  approvedAmount: number
}
export interface ApprovalResponse {
  transactionId: string
  childUserId: string
  approvedAmount: number
  approvedByUserId: string
  overrideUntil: string
}

/* ── auth / onboarding ─────────────────────────────────────── */
export interface InviteRequest {
  groupId: string
  username: string
  email?: string
  firstName?: string
  lastName?: string
  role: Role
  dailyLimit?: number
}
export interface InviteResponse {
  userId: string
  username: string
  temporaryPassword: string
  role: string
  groupId: string
  message: string
}

/* ── offers ────────────────────────────────────────────────── */
export interface Offer {
  partner: string
  title: string
  reward: string
}
