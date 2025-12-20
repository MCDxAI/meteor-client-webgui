<template>
  <div class="module-card-compact-wrapper" :class="{ active: module.active }">
    <div
      class="module-card-compact"
      role="button"
      tabindex="0"
      @click="handleCardToggle"
      @keydown.enter.prevent="handleCardToggle"
      @keydown.space.prevent="handleCardToggle"
    >
      <div class="compact-header">
        <div class="name-block">
          <h4>{{ module.title }}</h4>
          <div class="compact-badges">
            <span class="chip chip-small">{{ module.addon }}</span>
            <span class="chip chip-small muted">{{ module.settingGroups?.length || 0 }}</span>
          </div>
        </div>
        <button
          class="favorite-button"
          :class="{ active: isFavorite }"
          @click.stop="toggleFavorite"
          :title="isFavorite ? 'Remove from favorites' : 'Add to favorites'"
        >
          <span class="sr-only">Toggle favorite</span>
        </button>
      </div>

      <div class="compact-body">
        <button class="btn btn-ghost" @click.stop="emit('open-settings', module)">Configure</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import type { ModuleInfo } from '../stores/modules'
import { useModulesStore } from '../stores/modules'
import { useWebSocketStore } from '../stores/websocket'

const props = defineProps<{ module: ModuleInfo }>()
const emit = defineEmits<{ (event: 'open-settings', module: ModuleInfo): void }>()
const modulesStore = useModulesStore()
const wsStore = useWebSocketStore()
const toggling = ref(false)
const isFavorite = computed(() => modulesStore.isFavorite(props.module.name))

function toggleFavorite() {
  const newFavorites = modulesStore.toggleFavorite(props.module.name)
  wsStore.sendFavoritesUpdate(newFavorites)
}

function toggleModule() {
  if (toggling.value) return
  toggling.value = true
  wsStore.send({
    type: 'module.toggle',
    data: { moduleName: props.module.name },
    id: `toggle-${Date.now()}`
  })
  setTimeout(() => (toggling.value = false), 250)
}

function handleCardToggle() {
  toggleModule()
}
</script>

<style scoped>

.module-card-compact-wrapper.active .module-card-compact {
  border-color: rgba(75, 166, 255, 0.45);
  background: var(--color-surface-2);
}

.module-card-compact {
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: var(--radius-sm);
  background: var(--color-surface-1);
  padding: 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  cursor: pointer;
}

.compact-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 0.5rem;
}

.name-block {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  flex: 1;
}

.name-block h4 {
  margin: 0;
  font-size: 1rem;
}

.compact-badges {
  display: flex;
  gap: 0.35rem;
  flex-wrap: wrap;
}

.compact-body {
  display: flex;
  justify-content: center;
}

.favorite-button {
  min-width: 32px;
  height: 32px;
  border-radius: var(--radius-sm);
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: var(--color-surface-2);
  flex-shrink: 0;
  transition: background var(--transition-base), border-color var(--transition-base);
}

.favorite-button.active {
  background: #ffd85e;
  border-color: #ffd85e;
  box-shadow: 0 0 10px rgba(255, 216, 94, 0.35);
}

.module-card-compact:focus-visible {
  outline: none;
  border-color: rgba(75, 166, 255, 0.55);
}

</style>
