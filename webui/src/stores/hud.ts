import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import type { ModuleInfo } from './modules'

export interface HudTextLine {
  text: string
  x: number
  y: number
  color: string
  shadow: boolean
  scale: number
}

export interface HudElementState extends ModuleInfo {
  group: string
  x: number
  y: number
  width: number
  height: number
  hasNonText?: boolean
  updatedAt?: number
  lines: HudTextLine[]
}

export interface HudInitialStatePayload {
  elements: HudElementState[]
  previews?: HudPreviewPayload[]
}

export interface HudPreviewPayload extends Partial<HudElementState> {
  name: string
  lines: HudTextLine[]
  hasNonText?: boolean
  updatedAt?: number
}

export const useHudStore = defineStore('hud', () => {
  const elements = ref<Record<string, HudElementState>>({})
  const loading = ref(true)

  function setInitialState(payload: HudInitialStatePayload) {
    const next: Record<string, HudElementState> = {}
    payload.elements?.forEach((element) => {
      next[element.name] = {
        ...element,
        lines: element.lines || [],
        addon: element.addon || 'HUD',
        category: element.category || 'HUD'
      }
    })
    elements.value = next
    if (payload.previews) {
      applyPreviewUpdate(payload.previews)
    }
    loading.value = false
  }

  function applyPreviewUpdate(previews?: HudPreviewPayload[]) {
    if (!previews?.length) return
    const map = elements.value
    for (const preview of previews) {
      const existing = map[preview.name]
      if (!existing) {
        map[preview.name] = {
          name: preview.name,
          title: preview.title || preview.name,
          description: preview.description || '',
          group: preview.group || 'HUD',
          category: 'HUD',
          addon: preview.group || 'HUD',
          active: preview.active ?? true,
          x: preview.x ?? 0,
          y: preview.y ?? 0,
          width: preview.width ?? 0,
          height: preview.height ?? 0,
          lines: preview.lines || [],
          hasNonText: preview.hasNonText,
          updatedAt: preview.updatedAt,
          settingGroups: []
        }
        continue
      }

      map[preview.name] = {
        ...existing,
        ...preview,
        lines: preview.lines || existing.lines
      }
    }
  }

  function applyStateChange(payload: { elementName: string; active: boolean; x?: number; y?: number; width?: number; height?: number }) {
    const element = elements.value[payload.elementName]
    if (!element) return
    element.active = payload.active
    if (typeof payload.x === 'number') element.x = payload.x
    if (typeof payload.y === 'number') element.y = payload.y
    if (typeof payload.width === 'number') element.width = payload.width
    if (typeof payload.height === 'number') element.height = payload.height
  }

  function updateSettingValue(
    elementName: string,
    settingName: string,
    value: any,
    visibility?: Record<string, boolean>
  ) {
    const element = elements.value[elementName]
    if (!element?.settingGroups) return
    for (const group of element.settingGroups) {
      for (const setting of group.settings) {
        if (setting.name === settingName) {
          setting.value = value
        }
        if (visibility && Object.prototype.hasOwnProperty.call(visibility, setting.name)) {
          setting.visible = Boolean(visibility[setting.name])
        }
      }
    }
  }

  const orderedElements = computed(() =>
    Object.values(elements.value).sort((a, b) => a.title.localeCompare(b.title))
  )

  return {
    elements,
    loading,
    orderedElements,
    setInitialState,
    applyPreviewUpdate,
    applyStateChange,
    updateSettingValue
  }
})
