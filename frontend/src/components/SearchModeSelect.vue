<script setup lang="ts" generic="T extends string">
import { computed, nextTick, onBeforeUnmount, onDeactivated, ref } from 'vue'
import { Check, ChevronDown } from '@lucide/vue'

/**
 * 自定义下拉选择（用于搜索模式）：替代原生 select，避免各平台弹层样式丑且不统一。
 * 弹层挂载到 body 并使用 fixed 定位，不会被搜索框、卡片的 overflow 裁切。
 */
const props = defineProps<{
  modelValue: T
  options: { value: T; label: string; short?: string }[]
  /** header：顶部导航；form：搜索页；home：首页。 */
  variant?: 'header' | 'form' | 'home'
}>()
const emit = defineEmits<{ 'update:modelValue': [value: T] }>()

const open = ref(false)
const trigger = ref<HTMLButtonElement | null>(null)
const menu = ref<HTMLElement | null>(null)
const position = ref({ top: 0, left: 0, minWidth: 0 })
const activeIndex = ref(0)
const menuId = `mode-menu-${Math.random().toString(36).slice(2, 8)}`
const kind = computed(() => props.variant ?? 'form')
const current = computed(() => props.options.find((item) => item.value === props.modelValue))
const menuStyle = computed(() => ({
  top: `${position.value.top}px`,
  left: `${position.value.left}px`,
  minWidth: `${position.value.minWidth}px`,
}))

/** 计算弹层位置：默认在触发按钮下方，下方空间不足且上方够用时翻到上方，并限制在屏幕内。 */
function placeMenu(initial = false) {
  const el = trigger.value
  if (!el) return
  const rect = el.getBoundingClientRect()
  const minWidth = Math.max(rect.width, 148)
  const pop = menu.value
  if (initial || !pop) {
    position.value = { top: rect.bottom + 6, left: rect.left, minWidth }
    return
  }
  const margin = 8
  const left = Math.max(margin, Math.min(rect.left, window.innerWidth - pop.offsetWidth - margin))
  let top = rect.bottom + 6
  if (top + pop.offsetHeight > window.innerHeight - margin && rect.top - pop.offsetHeight - 6 >= margin) {
    top = rect.top - pop.offsetHeight - 6
  }
  position.value = { top, left, minWidth }
}

function onOutsidePointer(event: PointerEvent) {
  const target = event.target as Node | null
  if (!target) return
  if (trigger.value?.contains(target) || menu.value?.contains(target)) return
  closeMenu()
}

function onKeydown(event: KeyboardEvent) {
  if (!open.value) return
  const count = props.options.length
  if (event.key === 'Escape') {
    event.preventDefault()
    closeMenu(true)
  } else if (event.key === 'ArrowDown') {
    event.preventDefault()
    activeIndex.value = (activeIndex.value + 1) % count
  } else if (event.key === 'ArrowUp') {
    event.preventDefault()
    activeIndex.value = (activeIndex.value - 1 + count) % count
  } else if (event.key === 'Enter') {
    event.preventDefault()
    const item = props.options[activeIndex.value]
    if (item) choose(item.value)
  } else if (event.key === 'Tab') {
    closeMenu()
  }
}

function onViewportChange() {
  if (open.value) placeMenu()
}

function addListeners() {
  document.addEventListener('pointerdown', onOutsidePointer, true)
  document.addEventListener('keydown', onKeydown)
  window.addEventListener('resize', onViewportChange)
  window.addEventListener('scroll', onViewportChange, true)
}

function removeListeners() {
  document.removeEventListener('pointerdown', onOutsidePointer, true)
  document.removeEventListener('keydown', onKeydown)
  window.removeEventListener('resize', onViewportChange)
  window.removeEventListener('scroll', onViewportChange, true)
}

async function openMenu() {
  if (open.value) return
  activeIndex.value = Math.max(0, props.options.findIndex((item) => item.value === props.modelValue))
  placeMenu(true)
  open.value = true
  await nextTick()
  if (!open.value) return
  placeMenu()
  addListeners()
}

function closeMenu(refocus = false) {
  open.value = false
  removeListeners()
  if (refocus) trigger.value?.focus()
}

function toggle() {
  if (open.value) closeMenu()
  else void openMenu()
}

function choose(value: T) {
  emit('update:modelValue', value)
  closeMenu(true)
}

onBeforeUnmount(removeListeners)
onDeactivated(() => closeMenu())
</script>

<template>
  <div :class="['mode-select', `variant-${kind}`]">
    <button
      ref="trigger"
      type="button"
      :class="['mode-trigger', { open }]"
      aria-haspopup="listbox"
      :aria-expanded="open"
      :aria-controls="menuId"
      aria-label="搜索模式"
      @click="toggle"
      @keydown.down.prevent="openMenu"
    >
      <span class="mode-trigger-label">{{ current?.short ?? current?.label }}</span>
      <ChevronDown class="mode-trigger-chevron" :size="14" aria-hidden="true" />
    </button>
    <Teleport to="body">
      <Transition name="mode-menu">
        <ul v-if="open" :id="menuId" ref="menu" class="mode-menu" role="listbox" aria-label="搜索模式" :style="menuStyle">
          <li
            v-for="(item, index) in options"
            :key="item.value"
            role="option"
            :aria-selected="item.value === modelValue"
            :class="['mode-option', { selected: item.value === modelValue, active: index === activeIndex }]"
            @mouseenter="activeIndex = index"
            @click="choose(item.value)"
          >
            <span>{{ item.label }}</span>
            <Check v-if="item.value === modelValue" class="mode-option-check" :size="16" aria-hidden="true" />
          </li>
        </ul>
      </Transition>
    </Teleport>
  </div>
</template>

<style scoped>
.mode-select { position: relative; display: inline-flex; flex-shrink: 0; }
.mode-trigger { display: inline-flex; align-items: center; gap: 5px; margin: 0; border: 0; font: inherit; white-space: nowrap; cursor: pointer; transition: background .15s, border-color .15s, box-shadow .15s, color .15s, transform var(--motion-micro, 140ms) var(--ease-micro, cubic-bezier(0.2, 0, 0, 1)); }
.mode-trigger:active { transform: scale(0.96); }
.mode-trigger:focus-visible { outline: 0; }
.mode-trigger-label { overflow: hidden; text-overflow: ellipsis; }
.mode-trigger-chevron { flex-shrink: 0; opacity: .7; transition: transform .18s; }
.mode-trigger.open .mode-trigger-chevron { transform: rotate(180deg); }

.variant-header .mode-trigger { padding: 3px 6px 3px 9px; border-radius: 6px; background: #e0e7ff; color: #3730a3; font-size: 11px; font-weight: 700; }
.variant-header .mode-trigger:hover, .variant-header .mode-trigger.open { background: #c7d2fe; color: #3730a3; }
.variant-header .mode-trigger:focus-visible { box-shadow: 0 0 0 2px #93c5fd; }
.variant-header .mode-trigger-chevron { width: 12px; height: 12px; }

.variant-form .mode-trigger { padding: 6px 9px 6px 11px; border: 1px solid #dbe3f0; border-radius: 10px; background: #f1f5f9; color: #1e293b; font-size: 13px; font-weight: 650; }
.variant-form .mode-trigger:hover { border-color: #c7d2e3; background: #e8eef6; color: #1e293b; }
.variant-form .mode-trigger.open, .variant-form .mode-trigger:focus-visible { border-color: #818cf8; background: #f1f5f9; box-shadow: 0 0 0 3px #6366f119; }
@media (max-width: 640px) {
  .variant-form .mode-trigger { padding: 6px 8px 6px 2px; border-width: 0 1px 0 0; border-radius: 0; background: transparent; box-shadow: none; font-size: 13px; }
  .variant-form .mode-trigger:hover, .variant-form .mode-trigger.open, .variant-form .mode-trigger:focus-visible { background: transparent; box-shadow: none; color: #2563eb; }
}

.variant-home .mode-trigger { padding: 6px 9px 6px 11px; border: 1px solid #dbe3f0; border-radius: 8px; background: #f1f5f9; color: #1e293b; font-size: 13px; font-weight: 650; }
.variant-home .mode-trigger:hover { border-color: #c7d2e3; background: #e8eef6; color: #1e293b; }
.variant-home .mode-trigger.open, .variant-home .mode-trigger:focus-visible { border-color: #417de2; background: #f1f5f9; box-shadow: 0 0 0 3px #2563eb18; }
@media (max-width: 520px) {
  .variant-home .mode-trigger { padding: 6px 8px 6px 2px; border-width: 0 1px 0 0; border-radius: 0; background: transparent; box-shadow: none; font-size: 13px; }
  .variant-home .mode-trigger:hover, .variant-home .mode-trigger.open, .variant-home .mode-trigger:focus-visible { background: transparent; box-shadow: none; color: #2563eb; }
}

.mode-menu { position: fixed; z-index: 1000; margin: 0; padding: 6px; list-style: none; border: 1px solid #e2e8f0; border-radius: 12px; background: #fff; box-shadow: 0 12px 32px rgba(15, 23, 42, .14), 0 2px 6px rgba(15, 23, 42, .06); }
.mode-option { display: flex; align-items: center; justify-content: space-between; gap: 18px; padding: 10px 12px; border-radius: 8px; color: #334155; font-size: 14px; font-weight: 600; white-space: nowrap; cursor: pointer; transition: background .14s ease, color .14s ease, transform .12s ease; }
.mode-option:active { transform: scale(0.98); background: #eff6ff; }
.mode-option.active { background: #f1f5f9; }
.mode-option.selected { color: #2563eb; }
.mode-option-check { flex-shrink: 0; color: #2563eb; }
@media (max-width: 640px) { .mode-option { padding: 13px 14px; font-size: 15px; } }

.mode-menu { transform-origin: top left; }
.mode-menu-enter-active { transition: opacity 180ms var(--ease-enter, cubic-bezier(0.22, 1, 0.36, 1)), transform 180ms var(--ease-enter, cubic-bezier(0.22, 1, 0.36, 1)); }
.mode-menu-leave-active { transition: opacity 130ms var(--ease-exit, cubic-bezier(0.4, 0, 1, 1)), transform 130ms var(--ease-exit, cubic-bezier(0.4, 0, 1, 1)); }
.mode-menu-enter-from, .mode-menu-leave-to { opacity: 0; transform: translateY(-4px) scale(0.96); }
@media (prefers-reduced-motion: reduce) { .mode-menu-enter-active, .mode-menu-leave-active, .mode-trigger, .mode-trigger-chevron { transition: none; } }
</style>