import { Routes, Route } from 'react-router-dom'
import Providers from './providers'
import MainShell from '../features/shell/MainShell'
import FamilyApp from '../features/family/FamilyApp'

export default function App() {
  return (
    <Providers>
      <Routes>
        <Route path="/" element={<MainShell />} />
        <Route path="/family" element={<FamilyApp />} />
      </Routes>
    </Providers>
  )
}
