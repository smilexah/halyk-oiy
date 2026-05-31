import { useFamily } from '../FamilyContext'

/** Smart-push shown to папа during the SOS school-lunch flow. */
export default function PushHost() {
  const { pushVisible, pushStage, approveSOS, denySOS } = useFamily()
  if (!pushVisible) return null

  return (
    <div className="absolute inset-x-4 top-[calc(150px+env(safe-area-inset-top))] z-[65] animate-[fade_.28s_ease]">
      <div className="overflow-hidden rounded-[20px] bg-card shadow-float">
        <div className="flex items-center gap-2 border-b border-line2 px-4 py-2.5 text-[11.5px]">
          <span>💚</span>
          <span className="font-bold">Halyk · Maqsat Family</span>
          <span className="ml-auto text-muted">сейчас</span>
        </div>
        <div className="p-4">
          <div className="text-[14px] font-extrabold">⚡ Мадеке в школьной столовой</div>
          <div className="mt-1.5 text-[12.5px] leading-snug text-muted">
            Терминал распознан как <b className="text-ink">«Образование / Столовая»</b>. Не хватает <b className="text-ink">500 ₸</b> до оплаты обеда (лимит 2 000 ₸).
          </div>
          {pushStage === 'loading' ? (
            <div className="mt-3 flex justify-center">
              <span className="h-6 w-6 animate-spin rounded-full border-2 border-line border-t-accent" />
            </div>
          ) : (
            <div className="mt-3 flex gap-2">
              <button onClick={approveSOS} className="flex-1 rounded-xl bg-accent px-3 py-2.5 text-[12.5px] font-bold text-white">
                Одобрить разово
              </button>
              <button onClick={denySOS} className="rounded-xl bg-neg px-4 py-2.5 text-[12.5px] font-bold text-white">
                Отклонить
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
