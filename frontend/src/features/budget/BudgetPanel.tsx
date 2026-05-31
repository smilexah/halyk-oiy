import { useState } from 'react'
import { useBudget } from './useBudget'
import OnboardView from './views/OnboardView'
import ManageView from './views/ManageView'
import RecoStories from './recos/RecoStories'
import { RECO_STORIES } from './recos/stories'

/** "Мои финансы" — AI budget. Onboard card or manage view (first-run wizard
 *  is an app-level overlay owned by OnboardingProvider). */
export default function BudgetPanel() {
  const b = useBudget()
  const [storiesOpen, setStoriesOpen] = useState(false)

  if (b.step === 'onboard') return <OnboardView onStart={b.start} />

  return (
    <>
      <ManageView
        mandatory={b.mandatory}
        reco={b.reco}
        recoFunded={b.recoFunded}
        toggleMandatory={b.toggleMandatory}
        payMandatory={b.payMandatory}
        setRecoLimit={b.setRecoLimit}
        recoCount={RECO_STORIES.length}
        showRecos={() => setStoriesOpen(true)}
        replay={b.replay}
      />
      {storiesOpen && <RecoStories onClose={() => setStoriesOpen(false)} />}
    </>
  )
}