import { useEffect, useRef, useState } from 'react'
import Sheet from '../../../shared/ui/Sheet'
import { fmt } from '../../../shared/lib/format'
import { cn } from '../../../shared/lib/cn'
import type { RecoItem } from '../model'

interface Msg {
  who: 'me' | 'bot'
  html: string
}

const GREETING =
  'Привет! Я веду ваш бюджет. Скажите, что поправить в рекомендациях — например «хочу меньше на еду» или «урезать такси».'

export default function AiChatSheet({
  open,
  onOpenChange,
  reco,
  currentFree,
  adjustReco,
}: {
  open: boolean
  onOpenChange: (o: boolean) => void
  reco: RecoItem[]
  currentFree: number
  adjustReco: (id: string, delta: number) => void
}) {
  const [msgs, setMsgs] = useState<Msg[]>([{ who: 'bot', html: GREETING }])
  const [input, setInput] = useState('')
  const bodyRef = useRef<HTMLDivElement>(null)

  // reset to greeting whenever the sheet is (re)opened
  useEffect(() => {
    if (open) setMsgs([{ who: 'bot', html: GREETING }])
  }, [open])

  useEffect(() => {
    bodyRef.current?.scrollTo({ top: bodyRef.current.scrollHeight })
  }, [msgs])

  const limitOf = (id: string) => reco.find((c) => c.id === id)?.limit ?? 0
  const push = (m: Msg) => setMsgs((prev) => [...prev, m])
  const reply = (html: string) => setTimeout(() => push({ who: 'bot', html }), 450)

  const QUICK = [
    {
      t: 'Хочу меньше тратить на еду',
      fn: () => {
        adjustReco('food', -10000)
        reply(`Снизил рекомендацию по <b>Еде</b> до ${fmt(Math.max(0, limitOf('food') - 10000))} ₸. Это +10 000 ₸ к свободным — можно отложить в цель 🛒`)
      },
    },
    {
      t: 'Урезать такси',
      fn: () => {
        adjustReco('taxi', -3000)
        reply(`Рекомендация по <b>Такси</b> теперь ${fmt(Math.max(0, limitOf('taxi') - 3000))} ₸, включу уведомление при превышении 🚗`)
      },
    },
    {
      t: 'Больше откладывать на цель',
      fn: () => {
        adjustReco('food', -5000)
        adjustReco('fun', -4000)
        reply(`Освободил 9 000 ₸ из гибких категорий — свободно ${fmt(currentFree + 9000)} ₸/мес. Срок до цели сократится 📉`)
      },
    },
    {
      t: 'Где я перетрачиваю?',
      fn: () => {
        const o = reco.find((c) => c.avg > c.limit)
        reply(
          o
            ? `По <b>${o.name}</b> ваша средняя трата ${fmt(o.avg)} ₸ выше рекомендации ${fmt(o.limit)} ₸ — здесь чаще всего выходите за рамки.`
            : 'По рекомендациям вы в среднем укладываетесь — хорошая дисциплина 👍',
        )
      },
    },
  ]

  const send = () => {
    const v = input.trim()
    if (!v) return
    push({ who: 'me', html: v })
    setInput('')
    reply('Понял вас. Подстроил рекомендации под этот запрос — закройте чат, чтобы увидеть обновлённые лимиты 👍')
  }

  return (
    <Sheet
      open={open}
      onOpenChange={onOpenChange}
      title={
        <span className="flex items-center gap-2.5">
          <span className="grid h-[30px] w-[30px] place-items-center rounded-[9px] bg-balance text-[15px]">🤖</span>
          AI-ассистент
        </span>
      }
    >
      <p className="mt-1 text-[12.5px] text-muted">Опишите словами, что изменить — я пересоберу рекомендации.</p>

      <div ref={bodyRef} className="mt-3 max-h-[40vh] space-y-2 overflow-y-auto [scrollbar-width:none]">
        {msgs.map((m, i) => (
          <div
            key={i}
            className={cn(
              'max-w-[85%] rounded-2xl px-3.5 py-2.5 text-[13px] leading-snug',
              m.who === 'me' ? 'ml-auto bg-accent text-white' : 'bg-bg text-ink',
            )}
            dangerouslySetInnerHTML={{ __html: m.html }}
          />
        ))}
      </div>

      <div className="mt-3 flex flex-wrap gap-2">
        {QUICK.map((qk) => (
          <button
            key={qk.t}
            onClick={() => {
              push({ who: 'me', html: qk.t })
              qk.fn()
            }}
            className="rounded-full border border-line px-3 py-1.5 text-[12px] font-semibold text-muted active:scale-95"
          >
            {qk.t}
          </button>
        ))}
      </div>

      <div className="mt-3 flex gap-2">
        <input
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && send()}
          placeholder="Напишите сообщение…"
          className="h-[46px] flex-1 rounded-[13px] border border-line bg-bg px-3.5 text-[14px] outline-none focus:border-accent"
        />
        <button onClick={send} className="grid h-[46px] w-[46px] shrink-0 place-items-center rounded-[13px] bg-accent text-lg text-white active:scale-95">
          ➤
        </button>
      </div>
    </Sheet>
  )
}
