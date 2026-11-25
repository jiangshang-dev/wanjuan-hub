<script setup>
defineProps({ title: String, subtitle: String })
</script>

<template>
  <main class="auth-page">
    <router-link class="auth-brand" to="/"><span class="brand-mark">卷</span>万卷技术社区</router-link>
    <section class="auth-card">
      <div class="auth-heading">
        <h1>{{ title }}</h1>
        <p>{{ subtitle }}</p>
      </div>
      <slot />
    </section>
    <p class="auth-copyright">连接知识，记录成长</p>
  </main>
</template>
