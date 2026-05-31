import type { ReactNode } from 'react'

/** "spark" section heading used between content blocks. */
export default function Section({
  title,
  tag,
  icon = '✦',
}: {
  title: string
  tag?: string
  icon?: ReactNode
}) {
  return (
    <div className="mx-0.5 mt-[26px] mb-[13px] flex items-center gap-[9px]">
      <span className="grid h-[26px] w-[26px] place-items-center rounded-lg bg-green-soft text-[13px] text-green">
        {icon}
      </span>
      <h2 className="text-[17px] font-extrabold tracking-[-0.02em]">{title}</h2>
      {tag && (
        <span className="ml-auto rounded-full bg-green-soft px-[9px] py-1 text-[11px] font-bold text-pos">
          {tag}
        </span>
      )}
    </div>
  )
}
