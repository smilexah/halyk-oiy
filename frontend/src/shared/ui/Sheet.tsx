import type { ReactNode } from 'react'
import * as Dialog from '@radix-ui/react-dialog'
import { usePortalContainer } from './portalContainer'

/**
 * Bottom sheet (replaces the prototype's `.sheet.show` + `#scrim`). Built on
 * Radix Dialog for focus trapping / escape / scrim. Portals into the device
 * frame so it's clipped to the phone, not the viewport.
 */
export default function Sheet({
  open,
  onOpenChange,
  title,
  children,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
  title?: ReactNode
  children: ReactNode
}) {
  const container = usePortalContainer()

  return (
    <Dialog.Root open={open} onOpenChange={onOpenChange}>
      <Dialog.Portal container={container ?? undefined}>
        <Dialog.Overlay className="absolute inset-0 z-50 bg-black/40 backdrop-blur-[2px] data-[state=open]:animate-[fade_.2s_ease]" />
        <Dialog.Content
          className="absolute inset-x-0 bottom-0 z-50 max-h-[86%] overflow-y-auto rounded-t-[26px] bg-card px-[18px] pb-[calc(20px+env(safe-area-inset-bottom))] pt-3 shadow-float
            data-[state=open]:animate-[sheetUp_.28s_cubic-bezier(.22,1,.36,1)] [scrollbar-width:none] focus:outline-none"
        >
          <div className="mx-auto mb-3 h-1 w-9 rounded-full bg-line" />
          {title && <Dialog.Title className="text-[17px] font-extrabold">{title}</Dialog.Title>}
          {children}
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  )
}
