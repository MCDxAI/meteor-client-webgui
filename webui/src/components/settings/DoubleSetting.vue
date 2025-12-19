<template>
  <div class="double-setting">
    <div class="setting-header">
      <span class="setting-title">{{ setting.title }}</span>
      <span class="setting-value" :class="{ clamped: wasClamped }">{{ formatValue(setting.value.value) }}</span>
    </div>
    <p class="setting-description">{{ setting.description }}</p>

    <input
      v-if="!setting.typeMetadata?.noSlider"
      type="range"
      :min="sliderMin"
      :max="sliderMax"
      :step="getStep()"
      :value="setting.value.value"
      @input="updateValue(parseFloat(($event.target as HTMLInputElement).value))"
    />

    <input
      type="number"
      :min="getMin()"
      :max="getMax()"
      :step="getStep()"
      :value="setting.value.value"
      @input="updateValue(parseFloat(($event.target as HTMLInputElement).value))"
      class="number-input"
      :class="{ clamped: wasClamped }"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import type { ModuleInfo, SettingMetadata } from '../../stores/modules'
import { useWebSocketStore } from '../../stores/websocket'

const props = defineProps<{
  module: ModuleInfo
  setting: SettingMetadata
}>()

const wsStore = useWebSocketStore()
const wasClamped = ref(false)

function getMin(): number {
  return props.setting.typeMetadata?.min ?? -999999999
}

function getMax(): number {
  return props.setting.typeMetadata?.max ?? 999999999
}

// Slider range that expands to include the current value
// This fixes the issue where Meteor's default sliderMin/sliderMax (0-10) is too narrow
const sliderMin = computed(() => {
  const metaSliderMin = props.setting.typeMetadata?.sliderMin ?? 0
  const currentValue = props.setting.value?.value ?? 0
  const absoluteMin = getMin()

  // Expand slider min to include current value, but never go below absolute min
  return Math.max(absoluteMin, Math.min(metaSliderMin, currentValue))
})

const sliderMax = computed(() => {
  const metaSliderMax = props.setting.typeMetadata?.sliderMax ?? 100
  const currentValue = props.setting.value?.value ?? 0
  const absoluteMax = getMax()

  // Expand slider max to include current value, but never go above absolute max
  return Math.min(absoluteMax, Math.max(metaSliderMax, currentValue))
})

function clamp(value: number, min: number, max: number): number {
  return Math.min(Math.max(value, min), max)
}

function getStep() {
  const decimals = props.setting.typeMetadata?.decimalPlaces || 2
  return Math.pow(10, -decimals)
}

function formatValue(value: number) {
  const decimals = props.setting.typeMetadata?.decimalPlaces || 2
  return value.toFixed(decimals)
}

function updateValue(value: number) {
  if (isNaN(value)) return

  const min = getMin()
  const max = getMax()
  const clampedValue = clamp(value, min, max)

  // Show feedback if value was clamped
  if (clampedValue !== value) {
    wasClamped.value = true
    setTimeout(() => { wasClamped.value = false }, 1000)
  }

  wsStore.send({
    type: 'setting.update',
    data: {
      moduleName: props.module.name,
      settingName: props.setting.name,
      value: { value: clampedValue }
    }
  })
}
</script>

<style scoped>
.double-setting {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.setting-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.setting-title {
  font-size: 0.9rem;
  color: #fff;
}

.setting-value {
  font-size: 0.875rem;
  color: #4ba6ff;
  font-weight: bold;
}

.setting-description {
  font-size: 0.8rem;
  color: #888;
}

input[type="range"] {
  width: 100%;
  height: 4px;
  background: #333;
  border-radius: 2px;
  outline: none;
}

input[type="range"]::-webkit-slider-thumb {
  appearance: none;
  width: 16px;
  height: 16px;
  background: #4ba6ff;
  border-radius: 50%;
  cursor: pointer;
}

.number-input {
  padding: 0.5rem;
  background: #2a2a2a;
  color: #fff;
  border: 1px solid #444;
  border-radius: 4px;
  font-size: 0.875rem;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.number-input.clamped {
  border-color: #f59e0b;
  box-shadow: 0 0 0 2px rgba(245, 158, 11, 0.2);
}

.setting-value.clamped {
  color: #f59e0b;
}
</style>
