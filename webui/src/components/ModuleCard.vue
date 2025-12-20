<template>
  <div class="module-card-wrapper" :class="{ active: module.active }">
    <article
      class="module-card"
      role="button"
      tabindex="0"
      @click="handleCardToggle"
      @keydown.enter.prevent="handleCardToggle"
      @keydown.space.prevent="handleCardToggle"
    >
      <header class="module-header">
        <button
          class="favorite-button"
          :class="{ active: isFavorite }"
          @click.stop="toggleFavorite"
          :title="isFavorite ? 'Remove from favorites' : 'Add to favorites'"
        >
          <span class="sr-only">Toggle favorite</span>
        </button>

        <div class="module-title">
          <h3>{{ module.title }}</h3>
        </div>

        <div class="header-badges">
          <span class="chip chip-small">{{ module.addon }}</span>
          <span class="chip chip-small muted">{{ module.settingGroups?.length || 0 }}</span>
        </div>
      </header>

      <p class="description">
        {{ module.description }}
      </p>

      <footer class="card-footer">
        <button class="btn btn-ghost" @click.stop="emit('open-settings', module)">
          Configure
        </button>
      </footer>
    </article>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import type { ModuleInfo } from '../stores/modules'
import { useWebSocketStore } from '../stores/websocket'
import { useModulesStore } from '../stores/modules'

const props = defineProps<{
  module: ModuleInfo
}>()

const emit = defineEmits<{
  (event: 'open-settings', module: ModuleInfo): void
}>()

const wsStore = useWebSocketStore()
const modulesStore = useModulesStore()
const toggling = ref(false)

const isFavorite = computed(() => modulesStore.isFavorite(props.module.name))

async function toggleModule() {
  if (toggling.value) return
  toggling.value = true

  wsStore.send({
    type: 'module.toggle',
    data: {
      moduleName: props.module.name
    },
    id: `toggle-${Date.now()}`
  })

  setTimeout(() => {
    toggling.value = false
  }, 300)
}

function handleCardToggle() {
  toggleModule()
}

function toggleFavorite() {
  const newFavorites = modulesStore.toggleFavorite(props.module.name)
  wsStore.sendFavoritesUpdate(newFavorites)
}
</script>

<style scoped>
.module-card-wrapper {
  position: relative;
  border-radius: var(--radius-md);
}

.module-card-wrapper.active .module-card {
  border-color: rgba(75, 166, 255, 0.5);
  background: var(--color-surface-2);
  box-shadow: 0 20px 50px rgba(40, 90, 150, 0.35);
}

.module-card {
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: var(--radius-md);
  padding: 1.25rem;
  background: var(--color-surface-1);
  display: flex;
  flex-direction: column;
  gap: 1rem;
  min-height: 230px;
  transition: border var(--transition-base), box-shadow var(--transition-base);
  box-shadow: var(--shadow-soft);
  cursor: pointer;
}

.module-card:hover {
  border-color: rgba(75, 166, 255, 0.35);
}

.module-card:focus-visible {
  outline: none;
  border-color: rgba(75, 166, 255, 0.6);
  box-shadow: 0 0 0 2px rgba(75, 166, 255, 0.3);
}

.module-header {
  display: grid;
  grid-template-columns: 36px 1fr auto;
  align-items: center;
  gap: 0.75rem;
}

.favorite-button {
  min-width: 36px;
  height: 36px;
  border-radius: var(--radius-sm);
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: var(--color-surface-2);
  flex-shrink: 0;
  transition: background var(--transition-base), border-color var(--transition-base);
}

.favorite-button.active {
  background: #ffd85e;
  border-color: #ffd85e;
  box-shadow: 0 0 12px rgba(255, 216, 94, 0.35);
}

.module-title {
  text-align: center;
}

.module-title h3 {
  margin: 0;
  font-size: 1.2rem;
}

.header-badges {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  align-items: flex-end;
}

.description {
  color: var(--color-text-muted);
  font-size: 0.9rem;
  margin: 0;
  text-align: center;
  min-height: 48px;
}

.chip.muted {
  background: transparent;
  border-color: rgba(255, 255, 255, 0.08);
  color: var(--color-text-muted);
}

.card-footer {
  display: flex;
  justify-content: center;
}

.card-footer .btn {
  min-width: 140px;
}
</style>
