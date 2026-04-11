import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export interface ModuleInfo {
  name: string
  title: string
  description: string
  category: string
  active: boolean
  addon: string
  settingGroups?: SettingGroup[]
}

export interface SettingGroup {
  name: string
  settings: SettingMetadata[]
}

export interface SettingMetadata {
  name: string
  title: string
  description: string
  type: string
  value: any
  defaultValue: any
  visible: boolean
  typeMetadata?: any
}

export const useModulesStore = defineStore('modules', () => {
  const byCategory = ref<Record<string, ModuleInfo[]>>({})
  const loading = ref(true)
  const error = ref<string | null>(null)
  const favorites = ref<string[]>([])

  const activeModules = computed(() => {
    const active: string[] = []
    Object.values(byCategory.value).forEach(modules => {
      modules.forEach(module => {
        if (module.active) {
          active.push(module.name)
        }
      })
    })
    return active
  })

  const categories = computed(() => Object.keys(byCategory.value).sort())

  const favoriteModules = computed(() => {
    if (!favorites.value.length) return []
    const resolved: ModuleInfo[] = []
    for (const name of favorites.value) {
      const module = getModule(name)
      if (module) {
        resolved.push(module)
      }
    }
    return resolved
  })

  function setInitialState(modules: Record<string, ModuleInfo[]>) {
    byCategory.value = modules
    loading.value = false
    error.value = null
  }

  function updateModuleState(moduleName: string, active: boolean) {
    // Find and update the module
    for (const category in byCategory.value) {
      const module = byCategory.value[category].find(m => m.name === moduleName)
      if (module) {
        module.active = active
        return
      }
    }
  }

  function updateSettingValue(
    moduleName: string,
    settingName: string,
    value: any,
    visibility?: Record<string, boolean>
  ) {
    // Find the module and update the setting
    for (const category in byCategory.value) {
      const module = byCategory.value[category].find(m => m.name === moduleName)
      if (module && module.settingGroups) {
        for (const group of module.settingGroups) {
          for (const setting of group.settings) {
            if (setting.name === settingName) {
              setting.value = value
            }
            if (visibility && Object.prototype.hasOwnProperty.call(visibility, setting.name)) {
              setting.visible = Boolean(visibility[setting.name])
            }
          }
        }
        return
      }
    }
  }

  /**
   * Set favorites from backend (Meteor is source of truth)
   */
  function setFavorites(favoriteNames: string[]) {
    favorites.value = Array.from(new Set(favoriteNames))
  }

  /**
   * Toggle a module's favorite status (optimistic update)
   * Returns the new favorites array for sending to backend
   */
  function toggleFavorite(moduleName: string): string[] {
    const next = new Set(favorites.value)
    if (next.has(moduleName)) {
      next.delete(moduleName)
    } else {
      next.add(moduleName)
    }
    favorites.value = Array.from(next)
    return favorites.value
  }

  function isFavorite(moduleName: string) {
    return favorites.value.includes(moduleName)
  }

  function getModule(moduleName: string): ModuleInfo | null {
    for (const category in byCategory.value) {
      const module = byCategory.value[category].find(m => m.name === moduleName)
      if (module) return module
    }
    return null
  }

  return {
    byCategory,
    loading,
    error,
    activeModules,
    categories,
    favoriteModules,
    favorites,
    setInitialState,
    updateModuleState,
    updateSettingValue,
    setFavorites,
    getModule,
    toggleFavorite,
    isFavorite
  }
})
