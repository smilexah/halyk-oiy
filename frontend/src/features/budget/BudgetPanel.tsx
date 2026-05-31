import { useBudget } from './useBudget'
import OnboardView from './views/OnboardView'
import ManageView from './views/ManageView'

/** "Мои финансы" — AI budget. Onboard card or manage view (first-run wizard
 *  is an app-level overlay owned by OnboardingProvider). */
export default function BudgetPanel() {
  const b = useBudget()

  if (b.step === 'onboard') return <OnboardView onStart={b.start} />

  return (
    <ManageView
      mandatory={b.mandatory}
      reco={b.reco}
      toggleMandatory={b.toggleMandatory}
      payMandatory={b.payMandatory}
      setRecoLimit={b.setRecoLimit}
      adjustReco={b.adjustReco}
      confirmPlan={b.confirmPlan}
      replay={b.replay}
    />
  )
}
