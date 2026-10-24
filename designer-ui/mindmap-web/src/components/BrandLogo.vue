<template>
  <svg
    class="brand-logo"
    viewBox="0 0 64 64"
    fill="none"
    role="img"
    :aria-label="alt"
  >
    <rect class="brand-logo-bg" width="64" height="64" />
    <path
      fill="#ffffff"
      fill-rule="evenodd"
      d="M20 14h14c11.5 0 20 8 20 18s-8.5 18-20 18H20V14zm9 9v18h5c6.2 0 11-4.7 11-9s-4.8-9-11-9H29z"
    />
  </svg>
</template>

<script setup>
defineProps({
  alt: { type: String, default: 'logo' }
})
</script>

<style scoped>
.brand-logo {
  display: block;
  flex-shrink: 0;
  object-fit: contain;
}
.brand-logo-bg {
  fill: var(--color-primary);
  rx: var(--radius);
}
</style>
