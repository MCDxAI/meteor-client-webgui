import { defineStore } from 'pinia'
import { ref } from 'vue'
import { useModulesStore } from './modules'
import { useHudStore } from './hud'

export interface WSMessage {
  type: string
  data: any
  id?: string
}

export interface RegistryData {
  blocks: {
    blocks: Array<{ id: string; namespace: string }>
    byNamespace: Record<string, string[]>
  }
  items: {
    items: Array<{ id: string; namespace: string }>
    byNamespace: Record<string, string[]>
  }
  entities: {
    entities: Array<{ id: string; namespace: string }>
    byNamespace: Record<string, string[]>
  }
  statusEffects: Array<{ id: string; namespace: string }>
  modules: {
    byCategory: Record<string, Array<{ name: string; title: string; category: string }>>
  }
  potions?: Array<{ id: string; namespace: string }>
}

export const useWebSocketStore = defineStore('websocket', () => {
  const ws = ref<WebSocket | null>(null)
  const connected = ref(false)
  const reconnecting = ref(false)
  const error = ref<string | null>(null)
  const registries = ref<RegistryData | null>(null)

  const modulesStore = useModulesStore()
  const hudStore = useHudStore()

  function connect() {
    if (ws.value && ws.value.readyState === WebSocket.OPEN) {
      console.log('WebSocket already connected')
      return
    }

    try {
      // Connect to WebSocket server on /ws path
      // In dev mode (localhost:3000), Vite proxies /ws to localhost:8080
      // In production, uses same host/port as the page was loaded from
      const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
      const host = window.location.host || 'localhost:8080'
      const wsUrl = `${protocol}//${host}/ws`
      console.log('Connecting to WebSocket:', wsUrl)

      ws.value = new WebSocket(wsUrl)

      ws.value.onopen = () => {
        console.log('WebSocket connected')
        connected.value = true
        reconnecting.value = false
        error.value = null
      }

      ws.value.onclose = () => {
        console.log('WebSocket disconnected')
        connected.value = false

        // Auto-reconnect after 3 seconds
        if (!reconnecting.value) {
          reconnecting.value = true
          setTimeout(() => {
            reconnecting.value = false
            connect()
          }, 3000)
        }
      }

      ws.value.onerror = (event) => {
        console.error('WebSocket error:', event)
        error.value = 'WebSocket connection error'
      }

      ws.value.onmessage = (event) => {
        try {
          const message: WSMessage = JSON.parse(event.data)
          handleMessage(message)
        } catch (e) {
          console.error('Failed to parse WebSocket message:', e)
        }
      }
    } catch (e) {
      console.error('Failed to connect WebSocket:', e)
      error.value = 'Failed to connect'
    }
  }

  function handleMessage(message: WSMessage) {
    console.log('Received message:', message.type, message.data)

    switch (message.type) {
      case 'initial.state':
        modulesStore.setInitialState(message.data.modules)
        if (message.data.hud) {
          hudStore.setInitialState(message.data.hud)
        }
        // Set favorites from backend (Meteor is source of truth)
        if (message.data.favorites) {
          modulesStore.setFavorites(message.data.favorites)
          console.log('Favorites loaded:', message.data.favorites.length)
        }
        // Store registry data
        if (message.data.registries) {
          registries.value = message.data.registries
          console.log('Registry data loaded:', Object.keys(message.data.registries))
        }
        break

      case 'module.state.changed':
        modulesStore.updateModuleState(
          message.data.moduleName,
          message.data.active
        )
        break

      case 'setting.value.changed':
        modulesStore.updateSettingValue(
          message.data.moduleName,
          message.data.settingName,
          message.data.value
        )
        break

      case 'favorites.state.changed':
        modulesStore.setFavorites(message.data.favorites)
        break

      case 'registry.data':
        // Handle lazy-loaded registry data
        if (!registries.value) {
          registries.value = {} as RegistryData
        }

        const regType = message.data.registryType
        console.log('Registry data received for:', regType)

        if (regType === 'blocks') {
          registries.value.blocks = message.data.blocks
        } else if (regType === 'items') {
          registries.value.items = message.data.items
        } else if (regType === 'entities') {
          registries.value.entities = message.data.entities
        } else if (regType === 'statusEffects') {
          registries.value.statusEffects = message.data.statusEffects
        } else if (regType === 'potions') {
          registries.value.potions = message.data.potions
        } else if (regType === 'modules') {
          registries.value.modules = message.data.modules
        }
        break

      case 'hud.preview.update':
        hudStore.applyPreviewUpdate(message.data.elements)
        break

      case 'hud.state.changed':
        hudStore.applyStateChange(message.data)
        break

      case 'hud.setting.value.changed':
        hudStore.updateSettingValue(message.data.elementName, message.data.settingName, message.data.value)
        break

      case 'error':
        console.error('Server error:', message.data.error)
        error.value = message.data.error
        break

      case 'pong':
        // Heartbeat response
        break

      default:
        console.log('Unknown message type:', message.type)
    }
  }

  function send(message: WSMessage) {
    if (ws.value && ws.value.readyState === WebSocket.OPEN) {
      ws.value.send(JSON.stringify(message))
    } else {
      console.error('WebSocket not connected')
      error.value = 'Not connected to server'
    }
  }

  function disconnect() {
    if (ws.value) {
      ws.value.close()
      ws.value = null
    }
    connected.value = false
  }

  function requestRegistry(type: string) {
    console.log('Requesting registry:', type)
    send({
      type: 'registry.request',
      data: { registry: type }
    })
  }

  function sendFavoritesUpdate(favorites: string[]) {
    send({
      type: 'favorites.update',
      data: { favorites }
    })
  }

  return {
    connected,
    reconnecting,
    error,
    registries,
    connect,
    disconnect,
    send,
    requestRegistry,
    sendFavoritesUpdate
  }
})
