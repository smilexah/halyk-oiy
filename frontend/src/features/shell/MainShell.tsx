import { useRef, useState } from 'react'
import DeviceFrame from '../../shared/ui/DeviceFrame'
import TabBar from './TabBar'
import { type ScreenId } from './nav'
import HomeScreen from '../home/HomeScreen'
import TransfersScreen from '../transfers/TransfersScreen'
import PaymentsScreen from '../payments/PaymentsScreen'
import CardsScreen from '../cards/CardsScreen'
import ProfileScreen from '../profile/ProfileScreen'
import { OnboardingProvider, useOnboarding } from '../budget/onboarding'
import FirstRun from '../budget/firstrun/FirstRun'

/** Renders the first-run wizard (lockscreen → … → success) as a device-level overlay. */
function FirstRunMount() {
  const { firstRun, closeFirstRun, completeFirstRun } = useOnboarding()
  if (!firstRun) return null
  return <FirstRun startAt={firstRun} onClose={closeFirstRun} onComplete={completeFirstRun} />
}

function MainShellInner() {
  const [screen, setScreen] = useState<ScreenId>('home')
  const scrollRef = useRef<HTMLElement>(null)

  const go = (id: ScreenId) => {
    setScreen(id)
    scrollRef.current?.scrollTo({ top: 0 })
  }

  return (
    <DeviceFrame>
      <main ref={scrollRef} className="relative flex-1 overflow-y-auto overflow-x-hidden [scrollbar-width:none]">
        {screen === 'home' && <HomeScreen onNavigate={go} />}
        {screen === 'transfers' && <TransfersScreen />}
        {screen === 'payments' && <PaymentsScreen />}
        {screen === 'cards' && <CardsScreen />}
        {screen === 'profile' && <ProfileScreen />}
      </main>
      <TabBar active={screen} onSwitch={go} />
      <FirstRunMount />
    </DeviceFrame>
  )
}

/** App A shell — the Halyk superapp. */
export default function MainShell() {
  return (
    <OnboardingProvider>
      <MainShellInner />
    </OnboardingProvider>
  )
}
